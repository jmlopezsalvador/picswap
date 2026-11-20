package com.picswap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.picswap.domain.hsvToArgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Dibujos esquemáticos de la app (un móvil con lo que se ve en cada paso). Acompañan a la guía y a las instrucciones. */
enum class Art { Effect, Gallery, Selection, Editor, Eyedropper, Viewer, Pinch, Hold, Settings, EdgeBand, Ready, PhotoTips }

private val Paper = Color(0xFFECE6D7)
private val Chroma = Color(0xFF00B140)
private val Night = Color(0xFF1B1C1E)
private val Card = Color(0xFF26282B)

/** Rectángulo del móvil dentro del dibujo, con ayudas para colocar cosas por fracciones. */
private class Ph(val l: Float, val t: Float, val w: Float, val h: Float) {
    fun x(f: Float) = l + w * f
    fun y(f: Float) = t + h * f
    fun p(fx: Float, fy: Float) = Offset(x(fx), y(fy))
}

@Composable
fun Illustration(art: Art, modifier: Modifier = Modifier, height: Dp = 190.dp) {
    Canvas(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)).background(Card)) {
        val ph = size.height * 0.88f
        val pw = ph * 0.5f
        val p = Ph((size.width - pw) / 2f, (size.height - ph) / 2f, pw, ph)
        when (art) {
            Art.Effect -> drawEffect(p)
            Art.Gallery -> { clipPhone(p) { drawGallery(p, selecting = false) }; phoneBorder(p) }
            Art.Selection -> { clipPhone(p) { drawGallery(p, selecting = true) }; phoneBorder(p) }
            Art.Editor -> { clipPhone(p) { drawEditor(p) }; phoneBorder(p) }
            Art.Eyedropper -> { clipPhone(p) { drawEyedropper(p) }; phoneBorder(p) }
            Art.Viewer -> drawViewer(p)
            Art.Pinch -> drawPinch(p)
            Art.Hold -> drawHold(p)
            Art.Settings -> drawSettings(p)
            Art.EdgeBand -> drawEdgeBand(p)
            Art.Ready -> drawReady(p)
            Art.PhotoTips -> drawPhotoTips()
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Piezas comunes
// ------------------------------------------------------------------------------------------------

private fun DrawScope.phone(p: Ph, fill: Color) {
    drawRoundRect(fill, Offset(p.l, p.t), Size(p.w, p.h), CornerRadius(p.w * 0.14f))
    phoneBorder(p)
}

/** Dibuja `block` recortado al contorno redondeado del móvil (para que nada se salga por las esquinas). */
private fun DrawScope.clipPhone(p: Ph, block: DrawScope.() -> Unit) {
    val path = Path().apply {
        addRoundRect(RoundRect(p.l, p.t, p.l + p.w, p.t + p.h, CornerRadius(p.w * 0.14f)))
    }
    clipPath(path, block = block)
}

/** Solo el marco del móvil (se vuelve a dibujar por encima si algo se sale por las esquinas). */
private fun DrawScope.phoneBorder(p: Ph) {
    drawRoundRect(Color(0xFF8A8F98), Offset(p.l, p.t), Size(p.w, p.h), CornerRadius(p.w * 0.14f), style = Stroke(width = p.w * 0.035f))
}

private fun DrawScope.paper(center: Offset, w: Float, h: Float, degrees: Float = 8f) {
    rotate(degrees, center) {
        drawRoundRect(Paper, Offset(center.x - w / 2, center.y - h / 2), Size(w, h), CornerRadius(w * 0.04f))
        drawLine(Color(0xFFC8C0AC), Offset(center.x, center.y - h / 2 + 2f), Offset(center.x, center.y + h / 2 - 2f), strokeWidth = w * 0.04f)
    }
}

/** Marca de un dedo tocando la pantalla. */
private fun DrawScope.finger(c: Offset, r: Float, color: Color = Color.White) {
    drawCircle(color.copy(alpha = 0.35f), r, c)
    drawCircle(color, r, c, style = Stroke(width = r * 0.18f))
}

private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, width: Float) {
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    val a = atan2(to.y - from.y, to.x - from.x)
    val head = width * 3.2f
    for (side in listOf(1f, -1f)) {
        val ang = a + PI.toFloat() - side * 0.45f
        drawLine(color, to, Offset(to.x + head * cos(ang), to.y + head * sin(ang)), strokeWidth = width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.bar(p: Ph, fx: Float, fy: Float, fw: Float, color: Color, thick: Float = 0.018f) {
    drawRoundRect(color, p.p(fx, fy), Size(p.w * fw, p.h * thick), CornerRadius(p.h * thick / 2))
}

private fun DrawScope.topBar(p: Ph, withDots: Boolean, highlightDots: Boolean) {
    bar(p, 0.08f, 0.075f, 0.28f, Color(0xFFE8EAED), 0.022f)
    if (withDots) {
        drawCircle(Color(0xFFE8EAED), p.w * 0.04f, p.p(0.54f, 0.085f), style = Stroke(p.w * 0.018f))
        drawCircle(Color(0xFFE8EAED), p.w * 0.025f, p.p(0.70f, 0.085f))
        for (i in 0..2) drawCircle(Color(0xFFE8EAED), p.w * 0.014f, p.p(0.88f, 0.067f + i * 0.018f))
        if (highlightDots) drawCircle(Color(0xFF2BB35F), p.w * 0.085f, p.p(0.88f, 0.085f), style = Stroke(p.w * 0.022f))
    }
}

// ------------------------------------------------------------------------------------------------
// Dibujos
// ------------------------------------------------------------------------------------------------

/** El efecto: el objeto sale del móvil, dejando solo el fondo plano. */
private fun DrawScope.drawEffect(p: Ph) {
    phone(p, Chroma)
    // Papel saliendo por la derecha, por encima del borde del móvil.
    paper(p.p(0.95f, 0.5f), p.w * 0.46f, p.h * 0.32f, 10f)
    finger(p.p(0.86f, 0.5f), p.w * 0.12f)
    arrow(p.p(0.3f, 0.5f), p.p(0.66f, 0.5f), Color.White, p.w * 0.03f)
    // Rayas de movimiento.
    for (i in -1..1) drawLine(Color.White.copy(alpha = 0.5f), p.p(1.12f, 0.5f + i * 0.07f), p.p(1.3f, 0.5f + i * 0.07f), strokeWidth = p.w * 0.02f, cap = StrokeCap.Round)
}

/** Galería de miniaturas; con `selecting` aparece la selección y las acciones de abajo. */
private fun DrawScope.drawGallery(p: Ph, selecting: Boolean) {
    phone(p, Color(0xFF131314))
    topBar(p, withDots = !selecting, highlightDots = false)
    val tiles = listOf(
        Color(0xFF3A3F44), Color(0xFF2F4F3A), Color(0xFF4A3F36),
        Color(0xFF2C3E50), Color(0xFF50433A), Color(0xFF3B3B52),
        Color(0xFF444B3A), Color(0xFF3A4A4A), Color(0xFF52403C),
    )
    val gap = p.w * 0.02f
    val tile = (p.w * 0.88f - gap * 2) / 3f
    for (i in tiles.indices) {
        val cx = p.x(0.06f) + (i % 3) * (tile + gap)
        val cy = p.y(0.14f) + (i / 3) * (tile + gap)
        val isFirst = i == 0
        drawRect(if (isFirst) Chroma else tiles[i], Offset(cx, cy), Size(tile, tile))
        if (isFirst) {
            paper(Offset(cx + tile / 2, cy + tile / 2), tile * 0.4f, tile * 0.5f, 8f)
            if (selecting) {
                drawRect(Color(0x552BB35F), Offset(cx, cy), Size(tile, tile))
                drawCircle(Color(0xFF2BB35F), tile * 0.17f, Offset(cx + tile * 0.24f, cy + tile * 0.24f))
                val c = Offset(cx + tile * 0.24f, cy + tile * 0.24f)
                val ck = Path().apply {
                    moveTo(c.x - tile * 0.08f, c.y); lineTo(c.x - tile * 0.02f, c.y + tile * 0.06f); lineTo(c.x + tile * 0.09f, c.y - tile * 0.07f)
                }
                drawPath(ck, Color.White, style = Stroke(tile * 0.035f, cap = StrokeCap.Round))
            }
        }
    }
    if (selecting) {
        // Barra de acciones: Eliminar y Quitar fondo.
        drawRect(Color(0xFF1F2021), p.p(0.0f, 0.86f), Size(p.w, p.h * 0.14f))
        val y = p.y(0.925f)
        // Papelera
        drawRect(Color.White, Offset(p.x(0.2f) - p.w * 0.035f, y - p.h * 0.012f), Size(p.w * 0.07f, p.h * 0.04f), style = Stroke(p.w * 0.012f))
        drawLine(Color.White, Offset(p.x(0.2f) - p.w * 0.05f, y - p.h * 0.02f), Offset(p.x(0.2f) + p.w * 0.05f, y - p.h * 0.02f), strokeWidth = p.w * 0.014f)
        // Varita (quitar fondo)
        drawLine(Color.White, Offset(p.x(0.66f), y + p.h * 0.03f), Offset(p.x(0.76f), y - p.h * 0.02f), strokeWidth = p.w * 0.016f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF2BB35F), p.w * 0.022f, Offset(p.x(0.8f), y - p.h * 0.035f))
        // Dedo con pulsación larga
        finger(Offset(p.x(0.16f), p.y(0.22f)), p.w * 0.1f)
    } else {
        // Botón + de añadir
        drawCircle(Color(0xFF2BB35F), p.w * 0.11f, p.p(0.8f, 0.9f))
        drawLine(Color.White, p.p(0.8f, 0.9f - 0.04f), p.p(0.8f, 0.9f + 0.04f), strokeWidth = p.w * 0.022f, cap = StrokeCap.Round)
        drawLine(Color.White, p.p(0.8f - 0.07f, 0.9f), p.p(0.8f + 0.07f, 0.9f), strokeWidth = p.w * 0.022f, cap = StrokeCap.Round)
    }
}

/** Fila de controles del editor: colores, cuentagotas y deslizadores. */
private fun DrawScope.editorPanel(p: Ph, y0: Float) {
    drawRect(Color(0xFF1F2021), p.p(0.0f, y0), Size(p.w, p.h * (1f - y0)))
    val cy = p.y(y0 + 0.055f)
    val colors = listOf(Color.Black, Color.White, Chroma, Color(0xFF0047BB))
    for (i in colors.indices) {
        val c = Offset(p.x(0.13f + i * 0.13f), cy)
        drawCircle(colors[i], p.w * 0.045f, c)
        drawCircle(if (i == 2) Color(0xFF2BB35F) else Color(0xFF9AA0A6), p.w * 0.045f, c, style = Stroke(p.w * if (i == 2) 0.016f else 0.008f))
    }
    // Color personalizado (arcoíris)
    val rc = Offset(p.x(0.13f + 4 * 0.13f), cy)
    drawCircle(Brush.sweepGradient((0..6).map { Color(hsvToArgb(it * 60f, 1f, 1f)) }, rc), p.w * 0.045f, rc)
    // Cuentagotas
    drawCircle(Color(0xFF9AA0A6), p.w * 0.052f, Offset(p.x(0.9f), cy), style = Stroke(p.w * 0.01f))
    drawLine(Color.White, Offset(p.x(0.875f), cy + p.w * 0.025f), Offset(p.x(0.915f), cy - p.w * 0.02f), strokeWidth = p.w * 0.014f, cap = StrokeCap.Round)
    // Deslizadores: tolerancia y desenfoque
    for ((yf, v) in listOf((y0 + 0.13f) to 0.4f, (y0 + 0.195f) to 0.18f)) {
        val ty = p.y(yf)
        drawRoundRect(Color(0xFF131314), Offset(p.x(0.12f), ty), Size(p.w * 0.76f, p.h * 0.012f), CornerRadius(p.h * 0.006f))
        drawRoundRect(Color(0xFF2BB35F), Offset(p.x(0.12f), ty), Size(p.w * 0.76f * v, p.h * 0.012f), CornerRadius(p.h * 0.006f))
        drawCircle(Color(0xFF2BB35F), p.w * 0.03f, Offset(p.x(0.12f + 0.76f * v), ty + p.h * 0.006f))
    }
}

private fun DrawScope.drawEditor(p: Ph) {
    phone(p, Color(0xFF131314))
    // Barra superior con «Guardar»
    bar(p, 0.1f, 0.06f, 0.3f, Color(0xFFE8EAED), 0.02f)
    drawRoundRect(Color(0xFF2BB35F), p.p(0.62f, 0.035f), Size(p.w * 0.3f, p.h * 0.05f), CornerRadius(p.h * 0.025f))
    // Vista previa con el color elegido
    drawRect(Chroma, p.p(0.0f, 0.11f), Size(p.w, p.h * 0.55f))
    paper(p.p(0.5f, 0.38f), p.w * 0.36f, p.h * 0.22f, 8f)
    editorPanel(p, 0.66f)
    phoneBorder(p)
}

private fun DrawScope.drawEyedropper(p: Ph) {
    phone(p, Color(0xFF131314))
    bar(p, 0.1f, 0.06f, 0.3f, Color(0xFFE8EAED), 0.02f)
    // Foto original: fondo gris con luz irregular
    drawRect(Brush.radialGradient(listOf(Color(0xFF4A4C50), Color(0xFF26282B)), p.p(0.5f, 0.38f), p.w * 0.9f), p.p(0.0f, 0.11f), Size(p.w, p.h * 0.55f))
    paper(p.p(0.5f, 0.4f), p.w * 0.36f, p.h * 0.22f, 8f)
    // Marca donde se toca para elegir el fondo
    val c = p.p(0.2f, 0.2f)
    drawCircle(Color.Black, p.w * 0.075f, c, style = Stroke(p.w * 0.03f))
    drawCircle(Color.White, p.w * 0.075f, c, style = Stroke(p.w * 0.014f))
    finger(p.p(0.27f, 0.25f), p.w * 0.09f)
    editorPanel(p, 0.66f)
    // Cuentagotas activado
    drawCircle(Color(0x992BB35F), p.w * 0.052f, p.p(0.9f, 0.66f + 0.055f))
    phoneBorder(p)
}

private fun DrawScope.drawViewer(p: Ph) {
    phone(p, Chroma)
    paper(p.p(0.42f, 0.55f), p.w * 0.4f, p.h * 0.26f, 8f)
    finger(p.p(0.42f, 0.55f), p.w * 0.1f)
    arrow(p.p(0.5f, 0.5f), p.p(0.98f, 0.1f), Color.White, p.w * 0.028f)
}

private fun DrawScope.drawPinch(p: Ph) {
    phone(p, Chroma)
    // Tamaño original (línea discontinua) y tamaño ampliado
    drawRoundRect(Color.White.copy(alpha = 0.5f), p.p(0.31f, 0.42f), Size(p.w * 0.38f, p.h * 0.24f), CornerRadius(p.w * 0.02f), style = Stroke(p.w * 0.01f))
    paper(p.p(0.5f, 0.54f), p.w * 0.6f, p.h * 0.38f, 0f)
    finger(p.p(0.3f, 0.6f), p.w * 0.085f)
    finger(p.p(0.7f, 0.48f), p.w * 0.085f)
    arrow(p.p(0.36f, 0.64f), p.p(0.18f, 0.74f), Color.White, p.w * 0.024f)
    arrow(p.p(0.64f, 0.44f), p.p(0.82f, 0.34f), Color.White, p.w * 0.024f)
}

/** Un dedo sobre la transparencia no cuenta; el que está sobre el objeto sí. */
private fun DrawScope.drawHold(p: Ph) {
    phone(p, Chroma)
    paper(p.p(0.55f, 0.45f), p.w * 0.4f, p.h * 0.26f, 8f)
    // Dedo sujetando sobre la transparencia: tachado
    val hold = p.p(0.2f, 0.82f)
    finger(hold, p.w * 0.1f)
    drawCircle(Color(0xFFE5484D), p.w * 0.16f, hold, style = Stroke(p.w * 0.022f))
    drawLine(Color(0xFFE5484D), Offset(hold.x - p.w * 0.11f, hold.y - p.w * 0.11f), Offset(hold.x + p.w * 0.11f, hold.y + p.w * 0.11f), strokeWidth = p.w * 0.022f)
    // Dedo que arrastra el objeto
    finger(p.p(0.55f, 0.45f), p.w * 0.1f)
    arrow(p.p(0.66f, 0.42f), p.p(0.97f, 0.3f), Color.White, p.w * 0.026f)
}

private fun DrawScope.drawSettings(p: Ph) {
    phone(p, Color(0xFF131314))
    bar(p, 0.1f, 0.06f, 0.3f, Color(0xFFE8EAED), 0.022f)
    bar(p, 0.08f, 0.14f, 0.55f, Color(0xFFE8EAED), 0.016f)
    val colors = listOf(Color(0xFF8B5A2B), Color.White, Chroma, Color(0xFF0047BB))
    for (i in colors.indices) drawCircle(colors[i], p.w * 0.075f, p.p(0.18f + i * 0.21f, 0.26f))
    bar(p, 0.08f, 0.38f, 0.5f, Color(0xFFE8EAED), 0.016f)
    for ((i, on) in listOf(true, false, false).withIndex()) {
        val y = 0.48f + i * 0.14f
        drawCircle(if (on) Color(0xFF2BB35F) else Color(0xFF9AA0A6), p.w * 0.04f, p.p(0.14f, y), style = Stroke(p.w * 0.014f))
        if (on) drawCircle(Color(0xFF2BB35F), p.w * 0.02f, p.p(0.14f, y))
        bar(p, 0.26f, y - 0.012f, 0.5f, Color(0xFFE8EAED), 0.014f)
        bar(p, 0.26f, y + 0.018f, 0.62f, Color(0xFF6A6F76), 0.01f)
    }
}

/** Modo «solo en el borde»: el objeto desaparece si se suelta dentro del marco pegado a los bordes. */
private fun DrawScope.drawEdgeBand(p: Ph) {
    phone(p, Chroma)
    val band = p.w * 0.16f
    // Marco del borde
    drawRoundRect(Color(0x66FFFFFF), Offset(p.l + band / 2, p.t + band / 2), Size(p.w - band, p.h - band), CornerRadius(p.w * 0.1f), style = Stroke(band))
    paper(p.p(0.38f, 0.5f), p.w * 0.36f, p.h * 0.22f, 8f)
    arrow(p.p(0.5f, 0.5f), p.p(0.84f, 0.5f), Color.White, p.w * 0.026f)
    finger(p.p(0.93f, 0.5f), p.w * 0.075f)
}

private fun DrawScope.drawReady(p: Ph) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(Color(0xFF2BB35F), size.height * 0.3f, c)
    val r = size.height * 0.3f
    val ck = Path().apply {
        moveTo(c.x - r * 0.42f, c.y + r * 0.02f); lineTo(c.x - r * 0.1f, c.y + r * 0.34f); lineTo(c.x + r * 0.46f, c.y - r * 0.3f)
    }
    drawPath(ck, Color.White, style = Stroke(r * 0.16f, cap = StrokeCap.Round))
}

/** Foto buena (fondo liso y distinto al objeto) frente a foto mala (fondo con dibujo y parecido al objeto). */
private fun DrawScope.drawPhotoTips() {
    val cardW = size.width * 0.38f
    val cardH = size.height * 0.78f
    val top = (size.height - cardH) / 2f
    val leftX = size.width * 0.09f
    val rightX = size.width * 0.53f
    val corner = CornerRadius(cardW * 0.08f)

    // Buena: fondo oscuro liso y papel claro
    drawRoundRect(Color(0xFF1E2024), Offset(leftX, top), Size(cardW, cardH), corner)
    paper(Offset(leftX + cardW / 2, top + cardH / 2), cardW * 0.42f, cardH * 0.46f, 8f)

    // Mala: fondo con dibujo y del mismo tono que el papel
    clipPath(Path().apply { addRoundRect(RoundRect(rightX, top, rightX + cardW, top + cardH, corner)) }) {
        drawRect(Color(0xFFD9CFB4), Offset(rightX, top), Size(cardW, cardH))
        val stripe = cardW / 7f
        for (i in 0..8) {
            drawLine(
                Color(if (i % 2 == 0) 0xFFB8A57E else 0xFFE6DCC2), Offset(rightX + i * stripe - stripe, top + cardH),
                Offset(rightX + i * stripe + cardW * 0.4f, top), strokeWidth = stripe * 0.6f,
            )
        }
    }
    paper(Offset(rightX + cardW / 2, top + cardH / 2), cardW * 0.42f, cardH * 0.46f, -6f)

    // Marcas de bien / mal
    val r = cardH * 0.11f
    val good = Offset(leftX + cardW - r * 0.2f, top + r * 0.2f)
    drawCircle(Color(0xFF2BB35F), r, good)
    drawPath(
        Path().apply {
            moveTo(good.x - r * 0.45f, good.y + r * 0.02f); lineTo(good.x - r * 0.1f, good.y + r * 0.38f); lineTo(good.x + r * 0.5f, good.y - r * 0.32f)
        },
        Color.White, style = Stroke(r * 0.2f, cap = StrokeCap.Round),
    )
    val bad = Offset(rightX + cardW - r * 0.2f, top + r * 0.2f)
    drawCircle(Color(0xFFE5484D), r, bad)
    drawLine(Color.White, Offset(bad.x - r * 0.38f, bad.y - r * 0.38f), Offset(bad.x + r * 0.38f, bad.y + r * 0.38f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(bad.x + r * 0.38f, bad.y - r * 0.38f), Offset(bad.x - r * 0.38f, bad.y + r * 0.38f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
}
