package com.picswap

import com.picswap.domain.cutoutBackground
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackgroundCutoutTest {
    private val w = 120
    private val h = 160

    private fun alpha(px: IntArray, x: Int, y: Int) = (px[y * w + x] ushr 24) and 0xFF

    /** Fondo gris oscuro con viñeteado suave + un "papel" claro en el centro + una mota de polvo. */
    private fun scene(): IntArray {
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val dx = (x - w / 2) / (w / 2f)
            val dy = (y - h / 2) / (h / 2f)
            val g = 22 + (14 * (1f - (dx * dx + dy * dy) / 2f)).toInt()
            px[y * w + x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or (g + 2)
        }
        for (y in 50..110) for (x in 35..85) px[y * w + x] = 0xFFECE6D7.toInt()
        for (y in 8..9) for (x in 8..9) px[y * w + x] = 0xFFFFFFFF.toInt() // mota
        return px
    }

    @Test fun backgroundBecomesTransparentAndObjectStaysOpaque() {
        val out = cutoutBackground(scene(), w, h, 40)
        assertEquals(0, alpha(out, 2, 2))
        assertEquals(0, alpha(out, w - 3, h - 3))
        assertEquals(0, alpha(out, w / 2, 20)) // fondo con viñeteado
        assertEquals(255, alpha(out, w / 2, 80)) // centro del objeto
        assertEquals(0xECE6D7, out[80 * w + w / 2] and 0xFFFFFF) // el color del objeto no cambia
    }

    @Test fun dustSpeckIsRemoved() {
        val out = cutoutBackground(scene(), w, h, 40)
        assertEquals(0, alpha(out, 8, 8))
    }

    @Test fun zeroToleranceLeavesImageUntouched() {
        val src = scene()
        assertTrue(cutoutBackground(src, w, h, 0) === src)
    }

    @Test fun edgeIsSoftened() {
        val out = cutoutBackground(scene(), w, h, 40)
        val a = (30..40).map { alpha(out, it, 80) }
        assertTrue(a.any { it in 1..254 }, "debe haber píxeles de transición en el borde: $a")
    }

    @Test fun objectTouchingTheBorderIsKept() {
        val px = scene()
        for (y in 60..100) for (x in 0..20) px[y * w + x] = 0xFFFF0000.toInt()
        val out = cutoutBackground(px, w, h, 40)
        assertEquals(255, alpha(out, 10, 80))
        assertEquals(0, alpha(out, 100, 20))
    }

    @Test fun eyedropperSeedRemovesBackgroundThatDoesNotTouchTheBorder() {
        // Marco rojo en todo el borde con el fondo gris dentro: sin cuentagotas el fondo no se alcanza.
        val px = IntArray(w * h) { 0xFFFF0000.toInt() }
        for (y in 10 until h - 10) for (x in 10 until w - 10) px[y * w + x] = 0xFF303030.toInt()
        val without = cutoutBackground(px, w, h, 40)
        assertEquals(0, alpha(without, 0, 0)) // el borde (rojo) se toma como fondo
        val seed = (h / 2) * w + w / 2
        val with = cutoutBackground(px, w, h, 40, refColor = 0xFF303030.toInt(), seedIndex = seed)
        assertEquals(0, alpha(with, w / 2, h / 2)) // el interior gris pasa a transparente
        assertEquals(255, alpha(with, 3, 3)) // el marco rojo se conserva
    }
}
