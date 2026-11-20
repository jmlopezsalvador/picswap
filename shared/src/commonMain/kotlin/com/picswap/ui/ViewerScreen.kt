package com.picswap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.picswap.domain.DragMode
import com.picswap.domain.DragSettings
import com.picswap.domain.applyPinch
import com.picswap.domain.computeExitTargetForRect
import com.picswap.domain.imageRectOnScreen
import com.picswap.domain.isOpaqueNear
import com.picswap.domain.isInEdgeBand
import com.picswap.domain.shouldCommitFollow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * Visor de una foto.
 * - `immersive` = true: pantalla completa total. Arrastrar la imagen en cualquier dirección la saca de la pantalla
 *   y deja solo el color de fondo (el efecto de magia). El comportamiento (arrastrar y soltar, solo en el borde,
 *   o acompañar como una galería) lo define `drag`.
 * - Un toque en cualquier sitio alterna entre pantalla completa y modo normal (barra superior con flecha atrás).
 */
@Composable
fun ViewerScreen(
    id: String,
    bgColor: Color,
    immersive: Boolean,
    onToggleChrome: () -> Unit,
    onBack: () -> Unit,
    imageVersion: Int,
    drag: DragSettings,
    loadFull: suspend (String) -> ImageBitmap?,
) {
    val image by produceState<ImageBitmap?>(null, id, imageVersion) { value = loadFull(id) }
    // Mascara de alfa de la imagen mostrada: sirve para saber si el dedo toca el objeto o la transparencia.
    val alphaMask by produceState<ByteArray?>(null, image) {
        val img = image
        value = if (img == null) null else withContext(Dispatchers.Default) {
            val px = pixelsOf(img)
            ByteArray(px.size) { (px[it] ushr 24).toByte() }
        }
    }
    var offset by remember { mutableStateOf(Offset.Zero) }   // desplazamiento del arrastre (el swap)
    var zoom by remember(id) { mutableStateOf(1f) }          // zoom con pellizco: se queda como lo dejes
    var pan by remember(id) { mutableStateOf(Offset.Zero) }  // desplazamiento al hacer zoom con dos dedos
    var gone by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize().background(bgColor)) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minDragPx = with(LocalDensity.current) { 12.dp.toPx() }

        if (!gone) {
            image?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x + offset.x
                        translationY = pan.y + offset.y
                    },
                )
            }
        }

        // Capa de gestos:
        // - Toque: alterna la barra.
        // - Dos dedos sobre el objeto: zoom (pellizco) y desplazamiento; el zoom se queda como lo dejes.
        // - Un dedo que empieza SOBRE EL OBJETO (no sobre la transparencia): arrastre / swap (solo en pantalla completa).
        //   Los dedos que tocan la transparencia se ignoran: asi se puede sujetar el movil con otros dedos.
        Box(
            Modifier.fillMaxSize()
                .tapWithoutDrag { onToggleChrome() }
                .pointerInput(immersive, gone, widthPx, heightPx, drag, alphaMask) {
                    if (gone) return@pointerInput
                    val img = image ?: return@pointerInput
                    val mask = alphaMask ?: return@pointerInput
                    val bandPx = drag.edgeDp.dp.toPx()
                    val minSpeedPx = 600.dp.toPx()  // lanzamiento rapido (px/s) en el modo acompanar
                    val fingerPx = 10.dp.toPx()     // un dedo no es un punto: margen para tocar el borde del objeto
                    val center = Offset(widthPx / 2f, heightPx / 2f)

                    /** Hay objeto visible bajo (o junto a) este punto de la pantalla? */
                    fun hitsObject(p: Offset): Boolean = isOpaqueNear(
                        mask, img.width, img.height, p.x, p.y, widthPx, heightPx, zoom, pan.x, pan.y, fingerPx,
                    )

                    /** Al soltar: decide si el objeto termina de salir o vuelve a su sitio, y lo anima. */
                    fun release(lastPos: Offset, velocity: Velocity) {
                        val start = offset
                        val exitVector: Offset? = when (drag.mode) {
                            // Arrastrar y soltar: sale al soltar (en cualquier sitio o solo junto al borde).
                            DragMode.Drop ->
                                if (!drag.edgeOnly || isInEdgeBand(lastPos.x, lastPos.y, widthPx, heightPx, bandPx)) start else null
                            // Acompanar: sale si el gesto es suficiente (distancia o lanzamiento); si no, vuelve.
                            DragMode.Follow ->
                                if (shouldCommitFollow(start.x, start.y, velocity.x, velocity.y, widthPx, heightPx, minSpeedPx)) {
                                    if (start.getDistance() >= 1f) start else Offset(velocity.x, velocity.y)
                                } else null
                        }
                        val minDrag = if (drag.mode == DragMode.Drop) minDragPx else 1f
                        // La imagen puede tener zoom o estar desplazada: tiene que salir entera de la pantalla.
                        val rect = imageRectOnScreen(img.width, img.height, widthPx, heightPx, zoom, pan.x, pan.y)
                        val target = exitVector?.let {
                            computeExitTargetForRect(it.x, it.y, rect, widthPx, heightPx, minDrag)
                        }
                        val end = if (target != null) Offset(target.dx, target.dy) else Offset.Zero
                        val fast = hypot(velocity.x, velocity.y) >= minSpeedPx
                        scope.launch {
                            animating = true
                            val ms = if (target == null) 180 else if (drag.mode == DragMode.Follow && fast) 180 else 260
                            animate(0f, 1f, animationSpec = tween(ms, easing = FastOutSlowInEasing)) { p, _ ->
                                offset = start + (end - start) * p
                            }
                            if (target != null) gone = true
                            offset = Offset.Zero
                            animating = false
                        }
                    }

                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        if (animating) return@awaitEachGesture

                        val active = ArrayList<PointerId>()  // dedos que empezaron sobre el objeto
                        var swapId: PointerId? = null        // el dedo que hace el arrastre
                        var swapStart = Offset.Zero
                        var origin: Offset? = null           // donde se supero el umbral de arrastre
                        var tracker = VelocityTracker()
                        var last = first.position
                        var pinched = false                  // hubo dos dedos: en este gesto ya no hay arrastre
                        var released = false

                        fun cancelSwap() {
                            origin = null
                            swapId = null
                            offset = Offset.Zero
                        }

                        fun register(c: PointerInputChange) {
                            if (!hitsObject(c.position)) return  // dedo sobre la transparencia: se ignora
                            if (active.isNotEmpty()) {
                                pinched = true
                                cancelSwap()
                            } else {
                                swapId = c.id
                                swapStart = c.position
                                origin = null
                                tracker = VelocityTracker().also { it.addPosition(c.uptimeMillis, c.position) }
                                last = c.position
                            }
                            active.add(c.id)
                        }

                        register(first)
                        while (true) {
                            val event = awaitPointerEvent()

                            // Dedos nuevos: solo cuentan si caen sobre el objeto.
                            for (c in event.changes) if (c.pressed && !c.previousPressed) register(c)

                            // Dedos que se levantan: si es el del arrastre, se suelta.
                            for (c in event.changes) {
                                if (!c.pressed && c.id in active) {
                                    if (c.id == swapId && origin != null && !pinched && !released && immersive) {
                                        last = c.position
                                        tracker.addPosition(c.uptimeMillis, c.position)
                                        released = true
                                        release(last, tracker.calculateVelocity())
                                    }
                                    active.remove(c.id)
                                }
                            }

                            val act = event.changes.filter { it.id in active && it.pressed }
                            if (act.size >= 2) {
                                // Pellizco con dos dedos sobre el objeto: zoom y desplazamiento.
                                val a = act[0]
                                val b = act[1]
                                val prevDist = (a.previousPosition - b.previousPosition).getDistance()
                                val curDist = (a.position - b.position).getDistance()
                                val zoomChange = if (prevDist > 1f) curDist / prevDist else 1f
                                val oldC = (a.previousPosition + b.previousPosition) / 2f
                                val newC = (a.position + b.position) / 2f
                                val st = applyPinch(
                                    zoom, pan.x, pan.y, zoomChange, oldC.x, oldC.y, newC.x, newC.y,
                                    center.x, center.y, 0.2f, 8f,
                                )
                                zoom = st.zoom
                                pan = Offset(st.panX, st.panY)
                                a.consume()
                                b.consume()
                            } else if (act.size == 1 && !pinched && !released && immersive && act[0].id == swapId) {
                                // Un dedo sobre el objeto: arrastre.
                                val c = act[0]
                                last = c.position
                                tracker.addPosition(c.uptimeMillis, c.position)
                                if (origin == null && (c.position - swapStart).getDistance() > viewConfiguration.touchSlop) {
                                    origin = c.position
                                }
                                origin?.let { o ->
                                    offset = c.position - o  // la imagen sigue al dedo
                                    c.consume()
                                }
                            }

                            if (event.changes.none { it.pressed }) break
                        }
                        // Gesto cancelado sin llegar a soltar el dedo del arrastre.
                        if (origin != null && !pinched && !released && immersive) release(last, tracker.calculateVelocity())
                    }
                },
        )

        // Barra superior de modo normal (el resto de iconos son de atrezo y no hacen nada).
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarIcon(onClick = onBack) { AppIcon(IconKind.Back, Color.White) }
                Spacer(Modifier.weight(1f))
                BarIcon(onClick = {}) { AppIcon(IconKind.Share, Color.White) }
                BarIcon(onClick = {}) { AppIcon(IconKind.Heart, Color.White) }
                BarIcon(onClick = {}) { AppIcon(IconKind.Info, Color.White) }
                BarIcon(onClick = {}) { AppIcon(IconKind.More, Color.White) }
            }
        }
    }
}

@Composable
private fun BarIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        content()
    }
}

/**
 * Toque simple: un solo dedo que se levanta enseguida sin haberse movido. Un arrastre que no sea del objeto
 * (por ejemplo, desde la transparencia) o un gesto con varios dedos NO cuenta como toque; asi no se alterna la
 * barra sin querer mientras se sujeta el movil.
 */
private fun Modifier.tapWithoutDrag(onTap: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        val timeout = viewConfiguration.longPressTimeoutMillis
        var invalid = false
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.any { it.id != down.id }) invalid = true  // aparecio otro dedo
            val c = event.changes.firstOrNull { it.id == down.id } ?: break
            if ((c.position - down.position).getDistance() > slop || c.isConsumed) invalid = true
            if (!c.pressed) {
                if (!invalid && c.uptimeMillis - down.uptimeMillis < timeout) onTap()
                break
            }
        }
    }
}
