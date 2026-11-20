package com.picswap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picswap.domain.hsvToArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Lado mayor (px) de la imagen sobre la que se calcula la vista previa en vivo. */
private const val PREVIEW_SIZE = 800

/**
 * Editor de «quitar fondo»: vista previa en vivo, color de relleno, tolerancia y cuentagotas.
 * - Color: el que se verá detrás del objeto (y el que queda al sacarlo del móvil). Cuatro colores predefinidos
 *   (editables en los ajustes) y un quinto botón con una paleta para elegir un color solo para esta foto.
 * - Tolerancia: cuánto fondo se retira.
 * - Cuentagotas: tocar una zona de la foto fija ese color como el fondo a retirar.
 */
@Composable
fun EditorScreen(
    presets: List<Long>,
    initialColor: Long,
    initialTolerance: Int,
    loadOriginal: suspend (Int) -> ImageBitmap?,
    onSave: suspend (EditParams) -> Unit,
    onBack: () -> Unit,
) {
    PlatformBackHandler(enabled = true, onBack = onBack)
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf<ImageBitmap?>(null) }
    var pixels by remember { mutableStateOf<IntArray?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var color by remember { mutableStateOf(initialColor) }
    var tolerance by remember { mutableStateOf(initialTolerance.coerceIn(1, 100).toFloat()) }
    var eyedropper by remember { mutableStateOf(false) }
    var pickX by remember { mutableStateOf<Float?>(null) }
    var pickY by remember { mutableStateOf<Float?>(null) }
    var refColor by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    // Color propio de esta foto (quinto botón). Si el último color usado no es ninguno de los predefinidos, se recupera aquí.
    var customColor by remember { mutableStateOf<Long?>(if (initialColor in presets) null else initialColor) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val img = loadOriginal(PREVIEW_SIZE) ?: return@LaunchedEffect
        source = img
        pixels = withContext(Dispatchers.Default) { pixelsOf(img) }
    }

    fun currentParams() = EditParams(color, tolerance.roundToInt(), refColor, pickX, pickY)

    // Vista previa en vivo: se recalcula al mover la tolerancia o fijar el cuentagotas.
    LaunchedEffect(pixels, tolerance.roundToInt(), refColor, pickX, pickY) {
        val px = pixels ?: return@LaunchedEffect
        val img = source ?: return@LaunchedEffect
        delay(25)  // si se sigue moviendo el deslizador, este cálculo se cancela y empieza otro
        val params = currentParams()
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

    Column(Modifier.fillMaxSize().background(PicColors.Bg)) {
        // ---- Barra superior ----
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                AppIcon(IconKind.Back, PicColors.TextPrimary)
            }
            Text(
                "Quitar fondo", color = PicColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Box(
                Modifier.padding(end = 8.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (preview != null && !saving) PicColors.Accent else PicColors.Surface)
                    .clickable(enabled = preview != null && !saving) {
                        scope.launch {
                            saving = true
                            onSave(currentParams())
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

        // ---- Vista previa (el fondo de este recuadro es el color elegido) ----
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color(color))) {
            val boxW = with(LocalDensity.current) { maxWidth.toPx() }
            val boxH = with(LocalDensity.current) { maxHeight.toPx() }
            val shown = preview ?: source
            // Rectángulo donde se dibuja la imagen (ContentScale.Fit): se usa para convertir toques en posiciones.
            val scale = if (shown != null) min(boxW / shown.width, boxH / shown.height) else 1f
            val drawW = (shown?.width ?: 0) * scale
            val drawH = (shown?.height ?: 0) * scale
            val left = (boxW - drawW) / 2f
            val top = (boxH - drawH) / 2f

            if (shown != null) {
                Image(
                    bitmap = shown,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier.fillMaxSize().pointerInput(eyedropper, source, pixels) {
                    detectTapGestures { tap ->
                        val src = source
                        val px = pixels
                        if (!eyedropper || src == null || px == null || drawW <= 0f) return@detectTapGestures
                        val fx = (tap.x - left) / drawW
                        val fy = (tap.y - top) / drawH
                        if (fx !in 0f..1f || fy !in 0f..1f) return@detectTapGestures
                        pickX = fx
                        pickY = fy
                        refColor = averageColor(px, src.width, src.height, (fx * src.width).toInt(), (fy * src.height).toInt())
                        eyedropper = false
                    }
                },
            )
            // Marca donde se tocó con el cuentagotas.
            val fx = pickX
            val fy = pickY
            if (fx != null && fy != null && shown != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(left + fx * drawW, top + fy * drawH)
                    drawCircle(Color.Black, 15.dp.toPx(), c, style = Stroke(4.dp.toPx()))
                    drawCircle(Color.White, 15.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                }
            }
            if (shown == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = PicColors.Accent)
            }
            if (eyedropper) {
                Text(
                    "Toca en la zona de fondo que quieres quitar",
                    color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                        .clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // ---- Panel de herramientas ----
        Column(
            Modifier.fillMaxWidth().background(PicColors.Surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
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
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tolerancia", color = PicColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.width(78.dp))
                Slider(
                    value = tolerance,
                    onValueChange = { tolerance = it },
                    valueRange = 1f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = PicColors.Accent,
                        activeTrackColor = PicColors.Accent,
                        inactiveTrackColor = PicColors.Bg,
                    ),
                )
                Text(
                    tolerance.roundToInt().toString(), color = PicColors.TextPrimary, fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp).width(30.dp),
                )
            }
            if (refColor != null) {
                Text(
                    "Fondo elegido con el cuentagotas · Volver a automático",
                    color = PicColors.Accent, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp).clickable {
                        refColor = null; pickX = null; pickY = null
                    },
                )
            } else {
                Text(
                    "Fondo detectado automáticamente desde los bordes",
                    color = PicColors.TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
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
