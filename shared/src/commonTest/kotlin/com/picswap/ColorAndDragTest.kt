package com.picswap

import com.picswap.domain.argbToHex
import com.picswap.domain.argbToHsv
import com.picswap.domain.hsvToArgb
import com.picswap.domain.isInEdgeBand
import com.picswap.domain.shouldCommitFollow
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorAndDragTest {
    @Test fun primaryColorsFromHsv() {
        assertEquals(0xFFFF0000.toInt(), hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), hsvToArgb(240f, 1f, 1f))
        assertEquals(0xFFFFFFFF.toInt(), hsvToArgb(200f, 0f, 1f))
        assertEquals(0xFF000000.toInt(), hsvToArgb(200f, 1f, 0f))
    }

    @Test fun hsvRoundTripKeepsTheColor() {
        for (c in listOf(0xFF8B5A2B, 0xFF00B140, 0xFF0047BB, 0xFF336699, 0xFF808080)) {
            val argb = c.toInt()
            val hsv = argbToHsv(argb)
            val back = hsvToArgb(hsv[0], hsv[1], hsv[2])
            for (shift in listOf(16, 8, 0)) {
                assertTrue(abs(((argb shr shift) and 0xFF) - ((back shr shift) and 0xFF)) <= 1, "color $c")
            }
        }
    }

    @Test fun hexIsUppercaseAndPadded() {
        assertEquals("#00B140", argbToHex(0xFF00B140.toInt()))
        assertEquals("#000000", argbToHex(0xFF000000.toInt()))
    }

    @Test fun edgeBandDetectsReleaseNearTheEdges() {
        assertTrue(isInEdgeBand(10f, 500f, 1000f, 2000f, 80f))
        assertTrue(isInEdgeBand(990f, 500f, 1000f, 2000f, 80f))
        assertTrue(isInEdgeBand(500f, 30f, 1000f, 2000f, 80f))
        assertTrue(isInEdgeBand(500f, 1990f, 1000f, 2000f, 80f))
        assertFalse(isInEdgeBand(500f, 1000f, 1000f, 2000f, 80f))
        assertFalse(isInEdgeBand(200f, 300f, 1000f, 2000f, 80f))
    }

    @Test fun followCommitsOnLongDragOrFastFlick() {
        // Arrastre largo (> 25 % del lado menor) y lento: se completa.
        assertTrue(shouldCommitFollow(300f, 0f, 10f, 0f, 1000f, 2000f, 2000f))
        // Arrastre corto y lento: vuelve.
        assertFalse(shouldCommitFollow(60f, 0f, 100f, 0f, 1000f, 2000f, 2000f))
        // Arrastre corto pero lanzamiento rápido en el mismo sentido: se completa.
        assertTrue(shouldCommitFollow(60f, 0f, 3000f, 0f, 1000f, 2000f, 2000f))
        // Lanzamiento rápido en sentido contrario: vuelve.
        assertFalse(shouldCommitFollow(60f, 0f, -3000f, 0f, 1000f, 2000f, 2000f))
    }
}
