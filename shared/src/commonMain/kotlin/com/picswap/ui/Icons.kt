package com.picswap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class IconKind { Back, Plus, Search, More, Share, Heart, Trash, Close, Check, Info, Sliders, Wand, Undo, Eyedropper, Edit }

/** Iconos dibujados con Canvas/Path sobre una cuadrícula de 24 × 24 (sin depender de material-icons). */
@Composable
fun AppIcon(kind: IconKind, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = 2f * s, cap = StrokeCap.Round)
        fun path(block: Path.(Float) -> Unit) = drawPath(Path().apply { block(s) }, tint, style = stroke)

        when (kind) {
            IconKind.Back -> {
                line(19f, 12f, 5f, 12f)
                path { k -> moveTo(11f * k, 5f * k); lineTo(4f * k, 12f * k); lineTo(11f * k, 19f * k) }
            }
            IconKind.Plus -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            IconKind.Search -> {
                drawCircle(tint, radius = 6.5f * s, center = p(10.5f, 10.5f), style = stroke)
                line(15.5f, 15.5f, 20.5f, 20.5f)
            }
            IconKind.More -> listOf(5f, 12f, 19f).forEach { drawCircle(tint, 1.9f * s, p(12f, it)) }
            IconKind.Share -> {
                listOf(p(18f, 5f), p(6f, 12f), p(18f, 19f)).forEach { drawCircle(tint, 2.6f * s, it, style = stroke) }
                line(8.2f, 10.8f, 15.8f, 6.2f)
                line(8.2f, 13.2f, 15.8f, 17.8f)
            }
            IconKind.Heart -> path { k ->
                moveTo(12f * k, 20f * k)
                cubicTo(4f * k, 14f * k, 3f * k, 9f * k, 6.5f * k, 6.5f * k)
                cubicTo(9f * k, 4.5f * k, 11.5f * k, 6f * k, 12f * k, 8f * k)
                cubicTo(12.5f * k, 6f * k, 15f * k, 4.5f * k, 17.5f * k, 6.5f * k)
                cubicTo(21f * k, 9f * k, 20f * k, 14f * k, 12f * k, 20f * k)
                close()
            }
            IconKind.Trash -> {
                line(4.5f, 7f, 19.5f, 7f)
                path { k -> moveTo(9f * k, 7f * k); lineTo(9f * k, 4.5f * k); lineTo(15f * k, 4.5f * k); lineTo(15f * k, 7f * k) }
                path { k -> moveTo(6.5f * k, 7f * k); lineTo(7.5f * k, 20f * k); lineTo(16.5f * k, 20f * k); lineTo(17.5f * k, 7f * k) }
            }
            IconKind.Close -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            IconKind.Check -> path { k -> moveTo(5f * k, 12.5f * k); lineTo(10f * k, 17.5f * k); lineTo(19f * k, 7f * k) }
            IconKind.Info -> {
                drawCircle(tint, radius = 9f * s, center = p(12f, 12f), style = stroke)
                line(12f, 11f, 12f, 16.5f)
                drawCircle(tint, 1.2f * s, p(12f, 7.7f))
            }
            IconKind.Wand -> {
                line(4.5f, 19.5f, 14f, 10f)
                line(18f, 3.5f, 18f, 9.5f); line(15f, 6.5f, 21f, 6.5f)
                line(19.5f, 14f, 19.5f, 17f); line(18f, 15.5f, 21f, 15.5f)
            }
            IconKind.Edit -> {
                path { k ->
                    moveTo(4f * k, 20f * k); lineTo(5f * k, 15.5f * k); lineTo(15.5f * k, 5f * k)
                    lineTo(19f * k, 8.5f * k); lineTo(8.5f * k, 19f * k); close()
                }
                line(13.5f, 7f, 17f, 10.5f)
            }
            IconKind.Undo -> {
                path { k -> moveTo(9f * k, 5.5f * k); lineTo(4f * k, 10.5f * k); lineTo(9f * k, 15.5f * k) }
                path { k ->
                    moveTo(4f * k, 10.5f * k); lineTo(13f * k, 10.5f * k)
                    cubicTo(18f * k, 10.5f * k, 20f * k, 13f * k, 20f * k, 16f * k)
                    cubicTo(20f * k, 18.5f * k, 19f * k, 19f * k, 18f * k, 19f * k)
                }
            }
            IconKind.Eyedropper -> {
                path { k ->
                    moveTo(4f * k, 20f * k); lineTo(5f * k, 16f * k); lineTo(14f * k, 7f * k)
                    lineTo(17f * k, 10f * k); lineTo(8f * k, 19f * k); close()
                }
                line(13f, 4.5f, 19.5f, 11f)
                line(15f, 6.5f, 18f, 3.5f); line(18f, 3.5f, 20.5f, 6f)
            }
            IconKind.Sliders -> {
                line(4f, 8f, 20f, 8f); line(4f, 16f, 20f, 16f)
                drawCircle(tint, 2.6f * s, p(9f, 8f))
                drawCircle(tint, 2.6f * s, p(15f, 16f))
            }
        }
    }
}
