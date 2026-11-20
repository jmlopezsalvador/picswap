package com.picswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picswap.domain.argbToHex
import com.picswap.domain.argbToHsv
import com.picswap.domain.hsvToArgb

/**
 * Paleta de color: cuadro de saturación/brillo + barra de tono. Devuelve el color elegido como ARGB opaco (Long).
 */
@Composable
fun ColorPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val start = remember { argbToHsv(initial.toInt()) }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }
    val argb = hsvToArgb(hue, sat, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PicColors.Surface,
        title = { Text("Elegir color", color = PicColors.TextPrimary) },
        text = {
            Column {
                // Cuadro: saturación (izquierda → derecha) y brillo (abajo → arriba).
                Box(
                    Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color.White, Color(hsvToArgb(hue, 1f, 1f)))))
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        .drawBehind {
                            val c = Offset(sat * size.width, (1f - value) * size.height)
                            drawCircle(Color.Black, 11.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                            drawCircle(Color.White, 11.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                        }
                        .dragPosition { p, w, h ->
                            sat = (p.x / w).coerceIn(0f, 1f)
                            value = 1f - (p.y / h).coerceIn(0f, 1f)
                        },
                )
                Spacer(Modifier.height(14.dp))
                // Barra de tono.
                Box(
                    Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient((0..6).map { Color(hsvToArgb(it * 60f, 1f, 1f)) }))
                        .drawBehind {
                            val x = hue / 360f * size.width
                            drawCircle(Color.Black, 12.dp.toPx(), Offset(x, size.height / 2), style = Stroke(3.dp.toPx()))
                            drawCircle(Color.White, 12.dp.toPx(), Offset(x, size.height / 2), style = Stroke(1.5.dp.toPx()))
                        }
                        .dragPosition { p, w, _ -> hue = (p.x / w).coerceIn(0f, 1f) * 360f },
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(argb))
                            .border(1.dp, PicColors.TextSecondary, CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(argbToHex(argb), color = PicColors.TextPrimary, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton({ onPick(argb.toLong() and 0xFFFFFFFFL) }) { Text("Aceptar", color = PicColors.Accent) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = PicColors.Accent) } },
    )
}

/** Llama a `onPos(posición, ancho, alto)` al tocar y mientras se arrastra dentro del componente. */
private fun Modifier.dragPosition(onPos: (Offset, Float, Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onPos(down.position, size.width.toFloat(), size.height.toFloat())
        do {
            val event = awaitPointerEvent()
            event.changes.forEach {
                if (it.pressed) {
                    onPos(it.position, size.width.toFloat(), size.height.toFloat())
                    it.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
