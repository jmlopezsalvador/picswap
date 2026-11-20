package com.picswap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picswap.domain.applyPinch
import com.picswap.domain.hsvToArgb
import com.picswap.domain.imageRectOnScreen
import com.picswap.domain.screenToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Lado mayor (px) de la imagen sobre la que se calcula la vista previa en vivo. */
private const val PREVIEW_SIZE = 800

/**
 * Editor de la foto. La foto ocupa TODA la pantalla, exactamente igual que en el visor, y la barra superior y los
 * controles van semitransparentes por encima (los controles se pueden plegar). Así el tamaño y la posición que se
 * dejan son los que se verán al abrir la foto.
 *
 * - Color: el que se verá detrás del objeto (y el que queda al sacarlo del móvil). Cuatro colores predefinidos
 *   (editables en los ajustes) y un quinto botón con una paleta para elegir un color solo para esta foto.
 * - Tolerancia: cuánto fondo se retira.
 * - Desenfoque (cantidad) y Rango del borde: suavizan SOLO el borde del recorte; el resto de la imagen no se toca.
 * - Cuentagotas: tocar una zona de la foto fija ese color como el fondo a retirar.
 * - Tamaño: pellizcar (y arrastrar) la foto para ajustar su tamaño y posición.
 * - Restaurar original (solo si la foto ya estaba editada): vuelve a la foto original.
 *
 * @param initial valores con los que se abre (los de la última edición de esta foto, o los últimos usados).
 */
@Composable
fun EditorScreen(
    presets: List<Long>,
    initial: EditParams,
    edited: Boolean,
    loadOriginal: suspend (Int) -> ImageBitmap?,
    onSave: suspend (EditParams) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    PlatformBackHandler(enabled = true, onBack = onBack)
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf<ImageBitmap?>(null) }
    var pixels by remember { mutableStateOf<IntArray?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var color by remember { mutableStateOf(initial.color) }
    var tolerance by remember { mutableStateOf(initial.tolerance.coerceIn(1, 100).toFloat()) }
    var feather by remember { mutableStateOf(initial.feather.coerceIn(0, 100).toFloat()) }                    // rango
    var featherAmount by remember { mutableStateOf(initial.featherAmount.coerceIn(0, 100).toFloat()) }       // cantidad
    var eyedropper by remember { mutableStateOf(false) }
    var pickX by remember { mutableStateOf(initial.seedX) }
    var pickY by remember { mutableStateOf(initial.seedY) }
    var refColor by remember { mutableStateOf(initial.refColor) }
    var saving by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(true) }
    var confirmRestore by remember { mutableStateOf(false) }
    // Color propio de esta foto (quinto botón). Si el color de partida no es ninguno de los predefinidos, se recupera aquí.
    var customColor by remember { mutableStateOf<Long?>(if (initial.color in presets) null else initial.color) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val img = loadOriginal(PREVIEW_SIZE) ?: return@LaunchedEffect
        source = img
        pixels = withContext(Dispatchers.Default) { pixelsOf(img) }
    }

    fun cutParams() = EditParams(
        color = color,
        tolerance = tolerance.roundToInt(),
        feather = feather.roundToInt(),
        refColor = refColor,
        seedX = pickX,
        seedY = pickY,
        featherAmount = featherAmount.roundToInt(),
    )

    // Vista previa en vivo: se recalcula al mover los deslizadores o fijar el cuentagotas.
    LaunchedEffect(pixels, tolerance.roundToInt(), feather.roundToInt(), featherAmount.roundToInt(), refColor, pickX, pickY) {
        val px = pixels ?: return@LaunchedEffect
        val img = source ?: return@LaunchedEffect
        delay(25)  // si se sigue moviendo el deslizador, este cálculo se cancela y empieza otro
        val params = cutParams()
        preview = withContext(Dispatchers.Default) {
            imageFromArgb(img.width, img.height, applyCutout(px, img.width, img.height, params))
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = customColor ?: color,
            onDismiss = { showPicker = false },
            onPick = { customColor = it; color = it; showPicker = false },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = PicColors.Surface,
            title = { Text("¿Restaurar la foto original?", color = PicColors.TextPrimary) },
            text = { Text("Se perderá el fondo y los ajustes que hayas aplicado a esta foto.", color = PicColors.TextSecondary) },
            confirmButton = {
                TextButton({ confirmRestore = false; onRestore() }) { Text("Restaurar", color = PicColors.Accent) }
            },
            dismissButton = { TextButton({ confirmRestore = false }) { Text("Cancelar", color = PicColors.Accent) } },
        )
    }

    // La pantalla entera es la «pantalla del visor»: la foto se ve a tamaño real y sin límites; el fondo es el color elegido.
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(color))) {
        val winW = constraints.maxWidth.toFloat()
        val winH = constraints.maxHeight.toFloat()
        val center = Offset(winW / 2f, winH / 2f)
        val shown = preview ?: source
        // Tamaño y posición de la foto (la vista con la que se abrirá en el visor).
        var zoom by remember { mutableStateOf(initial.zoom) }
        var pan by remember { mutableStateOf(Offset(initial.panX * winW, initial.panY * winH)) }

        if (shown != null) {
            Image(
                bitmap = shown,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                },
            )
        }

        // Gestos sobre la foto: toque (cuentagotas) y pellizco / arrastre (tamaño y posición).
        Box(
            Modifier.fillMaxSize()
                .pointerInput(eyedropper, source, pixels, winW, winH) {
                    detectTapGestures { tap ->
                        val src = source
                        val px = pixels
                        if (!eyedropper || src == null || px == null) return@detectTapGestures
                        val hit = screenToImage(src.width, src.height, tap.x, tap.y, winW, winH, zoom, pan.x, pan.y)
                            ?: return@detectTapGestures
                        pickX = hit.first
                        pickY = hit.second
                        refColor = averageColor(px, src.width, src.height, (hit.first * src.width).toInt(), (hit.second * src.height).toInt())
                        eyedropper = false
                    }
                }
                .pointerInput(winW, winH) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val st = applyPinch(
                            zoom, pan.x, pan.y, zoomChange, centroid.x - panChange.x, centroid.y - panChange.y,
                            centroid.x, centroid.y, center.x, center.y, 0.2f, 8f,
                        )
                        zoom = st.zoom
                        pan = Offset(st.panX, st.panY)
                    }
                },
        )

        // Marca donde se tocó con el cuentagotas (sigue a la imagen al hacer zoom).
        val fx = pickX
        val fy = pickY
        val src = source
        if (fx != null && fy != null && src != null) {
            Canvas(Modifier.fillMaxSize()) {
                val r = imageRectOnScreen(src.width, src.height, winW, winH, zoom, pan.x, pan.y)
                val c = Offset(r.left + fx * (r.right - r.left), r.top + fy * (r.bottom - r.top))
                drawCircle(Color.Black, 15.dp.toPx(), c, style = Stroke(4.dp.toPx()))
                drawCircle(Color.White, 15.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            }
        }
        if (shown == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = PicColors.Accent)
        }

        // ---- Barra superior (encima de la foto) ----
        Column(
            Modifier.align(Alignment.TopStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    AppIcon(IconKind.Back, Color.White)
                }
                Text(
                    "Editar", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Box(
                    Modifier.padding(end = 8.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (preview != null && !saving) PicColors.Accent else Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = preview != null && !saving) {
                            scope.launch {
                                saving = true
                                onSave(cutParams().copy(zoom = zoom, panX = pan.x / winW, panY = pan.y / winH))
                                saving = false
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Guardar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (eyedropper) {
                    Text(
                        "Toca en la zona de fondo que quieres quitar",
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                } else {
                    Text(
                        "Pellizca para ajustar el tamaño",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (zoom != 1f || pan != Offset.Zero) {
                    Text(
                        "Restablecer tamaño",
                        color = Color.White, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .clickable { zoom = 1f; pan = Offset.Zero }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // ---- Controles (encima de la foto; se pueden plegar) ----
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (!panelOpen) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(20.dp)).background(PicColors.Surface.copy(alpha = 0.92f))
                        .clickable { panelOpen = true }.padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Mostrar controles", color = PicColors.TextPrimary, fontSize = 14.sp) }
            } else {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(PicColors.Surface.copy(alpha = 0.92f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp),
                ) {
                    // Asa para plegar los controles
                    Box(Modifier.fillMaxWidth().clickable { panelOpen = false }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(PicColors.TextSecondary))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Color", color = PicColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.width(44.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { argb ->
                                Swatch(argb, selected = color == argb) { color = argb }
                            }
                            // Quinto botón: abre una paleta para elegir un color solo para esta foto.
                            // Sin color elegido se ve como un arcoíris; con color elegido, se ve de ese color.
                            val custom = customColor
                            val selected = custom != null && color == custom
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(
                                        if (custom != null) Brush.linearGradient(listOf(Color(custom), Color(custom)))
                                        else Brush.sweepGradient((0..6).map { Color(hsvToArgb(it * 60f, 1f, 1f)) }),
                                    )
                                    .border(if (selected) 3.dp else 1.dp, if (selected) PicColors.Accent else PicColors.TextSecondary, CircleShape)
                                    .clickable { showPicker = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected && custom != null) AppIcon(IconKind.Check, if (isLight(custom)) Color.Black else Color.White, 16.dp)
                            }
                        }
                        // Cuentagotas: tocar la foto para elegir el color de fondo a retirar.
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (eyedropper) PicColors.Accent else Color.Transparent)
                                .border(1.5.dp, if (eyedropper) PicColors.Accent else PicColors.TextSecondary, CircleShape)
                                .clickable { eyedropper = !eyedropper },
                            contentAlignment = Alignment.Center,
                        ) { AppIcon(IconKind.Eyedropper, if (eyedropper) Color.White else PicColors.TextPrimary, 22.dp) }
                    }
                    Spacer(Modifier.height(6.dp))
                    SliderRow("Tolerancia", tolerance, 1f..100f) { tolerance = it }
                    SliderRow("Desenfoque", featherAmount, 0f..100f) { featherAmount = it }
                    SliderRow("Rango del borde", feather, 0f..100f) { feather = it }
                    if (refColor != null) {
                        Text(
                            "Fondo elegido con el cuentagotas · Volver a automático",
                            color = PicColors.Accent, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp).clickable {
                                refColor = null; pickX = null; pickY = null
                            },
                        )
                    } else {
                        Text(
                            "Fondo detectado automáticamente desde los bordes",
                            color = PicColors.TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (edited) {
                        Row(
                            Modifier.padding(top = 10.dp).clip(RoundedCornerShape(14.dp))
                                .border(1.dp, PicColors.TextSecondary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .clickable { confirmRestore = true }.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(IconKind.Undo, PicColors.TextPrimary, 18.dp)
                            Text("  Restaurar foto original", color = PicColors.TextPrimary, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

/** Fila con nombre, deslizador y valor. */
@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PicColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.width(112.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = PicColors.Accent,
                activeTrackColor = PicColors.Accent,
                inactiveTrackColor = PicColors.Bg,
            ),
        )
        Text(
            value.roundToInt().toString(), color = PicColors.TextPrimary, fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp).width(30.dp),
        )
    }
}

/** Color medio de un cuadrado de 5 × 5 píxeles alrededor de (x, y): evita elegir un píxel con ruido. */
private fun averageColor(pixels: IntArray, w: Int, h: Int, x: Int, y: Int): Int {
    var r = 0
    var g = 0
    var b = 0
    var n = 0
    for (yy in (y - 2)..(y + 2)) {
        for (xx in (x - 2)..(x + 2)) {
            if (xx !in 0 until w || yy !in 0 until h) continue
            val c = pixels[yy * w + xx]
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
            n++
        }
    }
    if (n == 0) return 0xFF000000.toInt()
    return (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
}

/** Botón redondo de color; con una marca de verificación si está elegido. */
@Composable
private fun Swatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val onColor = if (isLight(argb)) Color.Black else Color.White
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(Color(argb))
            .border(if (selected) 3.dp else 1.dp, if (selected) PicColors.Accent else PicColors.TextSecondary, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) AppIcon(IconKind.Check, onColor, 16.dp)
    }
}

/** ¿Es un color claro? (para elegir un icono negro o blanco encima). */
internal fun isLight(argb: Long): Boolean {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return 0.299 * r + 0.587 * g + 0.114 * b > 150
}
