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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.picswap.domain.DragMode
import com.picswap.domain.DragSettings
import com.picswap.domain.computeExitTarget
import com.picswap.domain.isInEdgeBand
import com.picswap.domain.shouldCommitFollow
import kotlinx.coroutines.launch
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
    var offset by remember { mutableStateOf(Offset.Zero) }
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
                        translationX = offset.x
                        translationY = offset.y
                    },
                )
            }
        }

        // Capa de gestos: toque (alterna la barra) y arrastre libre (solo en pantalla completa).
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onToggleChrome() }) }
                .pointerInput(immersive, gone, widthPx, heightPx, drag) {
                    if (!immersive || gone) return@pointerInput
                    val bandPx = drag.edgeDp.dp.toPx()
                    val minSpeedPx = 600.dp.toPx()  // lanzamiento rápido (px/s) en el modo «acompañar»

                    /** Al soltar: decide si el objeto termina de salir o vuelve a su sitio, y lo anima. */
                    fun release(lastPos: Offset, velocity: Velocity) {
                        val start = offset
                        val exitVector: Offset? = when (drag.mode) {
                            // Arrastrar y soltar: sale al soltar (en cualquier sitio o solo junto al borde).
                            DragMode.Drop ->
                                if (!drag.edgeOnly || isInEdgeBand(lastPos.x, lastPos.y, widthPx, heightPx, bandPx)) start else null
                            // Acompañar: sale si el gesto es suficiente (distancia o lanzamiento); si no, vuelve.
                            DragMode.Follow ->
                                if (shouldCommitFollow(start.x, start.y, velocity.x, velocity.y, widthPx, heightPx, minSpeedPx)) {
                                    if (start.getDistance() >= 1f) start else Offset(velocity.x, velocity.y)
                                } else null
                        }
                        val minDrag = if (drag.mode == DragMode.Drop) minDragPx else 1f
                        val target = exitVector?.let { computeExitTarget(it.x, it.y, widthPx, heightPx, minDrag) }
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
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (animating) return@awaitEachGesture
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var origin: Offset? = null  // dónde se superó el umbral de arrastre
                        var last = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            last = change.position
                            tracker.addPosition(change.uptimeMillis, change.position)
                            if (!change.pressed) break
                            if (origin == null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                origin = change.position
                            }
                            origin?.let { o ->
                                offset = change.position - o  // la imagen sigue al dedo
                                change.consume()
                            }
                        }
                        if (origin != null) release(last, tracker.calculateVelocity())
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
