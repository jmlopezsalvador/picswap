package com.picswap.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** HSV (h: 0-360, s y v: 0-1) → color ARGB opaco (`0xFFRRGGBB`). */
fun hsvToArgb(h: Float, s: Float, v: Float): Int {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun to255(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)
}

/** Color ARGB → [h (0-360), s (0-1), v (0-1)]. */
fun argbToHsv(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return floatArrayOf((h + 360f) % 360f, if (mx == 0f) 0f else d / mx, mx)
}

/** `#RRGGBB` en mayúsculas. */
fun argbToHex(argb: Int): String {
    fun h2(v: Int) = v.toString(16).padStart(2, '0').uppercase()
    return "#" + h2((argb shr 16) and 0xFF) + h2((argb shr 8) and 0xFF) + h2(argb and 0xFF)
}
