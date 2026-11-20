package com.picswap

import com.picswap.domain.ImageRect
import com.picswap.domain.applyPinch
import com.picswap.domain.computeExitTargetForRect
import com.picswap.domain.imageRectOnScreen
import com.picswap.domain.isOpaqueNear
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZoomAndHitTest {
    // Imagen de 100 × 100 px con un cuadrado opaco de 30 × 30 en el centro (35..64).
    private val imgW = 100
    private val imgH = 100
    private val alpha = ByteArray(imgW * imgH).also { a ->
        for (y in 35..64) for (x in 35..64) a[y * imgW + x] = 0xFF.toByte()
    }

    // Pantalla de 1000 × 2000: la imagen se ajusta al ancho (escala 10) y queda centrada: y de 500 a 1500.
    private val sw = 1000f
    private val sh = 2000f

    private fun hit(x: Float, y: Float, zoom: Float = 1f, panX: Float = 0f, panY: Float = 0f, r: Float = 0f) =
        isOpaqueNear(alpha, imgW, imgH, x, y, sw, sh, zoom, panX, panY, r)

    @Test fun rectOfFittedImage() {
        val r = imageRectOnScreen(imgW, imgH, sw, sh, 1f, 0f, 0f)
        assertEquals(0f, r.left, 0.01f)
        assertEquals(1000f, r.right, 0.01f)
        assertEquals(500f, r.top, 0.01f)
        assertEquals(1500f, r.bottom, 0.01f)
    }

    @Test fun rectScalesAroundScreenCenterAndPans() {
        val r = imageRectOnScreen(imgW, imgH, sw, sh, 2f, 30f, -20f)
        // Mitad del ancho de la imagen escalada: 1000 → el rectángulo mide 2000 y está centrado en (500 + 30, 1000 - 20).
        assertEquals(-470f, r.left, 0.01f)
        assertEquals(1530f, r.right, 0.01f)
        assertEquals(-20f, r.top, 0.01f)
        assertEquals(1980f, r.bottom, 0.01f)
    }

    @Test fun touchOnObjectHitsAndOnTransparencyDoesNot() {
        assertTrue(hit(500f, 1000f))   // centro del objeto
        assertFalse(hit(100f, 1000f))  // transparencia dentro de la imagen
        assertFalse(hit(500f, 100f))   // fuera de la imagen (franja)
    }

    @Test fun fingerRadiusAllowsTouchingJustOutsideTheEdge() {
        // El objeto ocupa x de 350 a 650 en pantalla. 20 px fuera del borde:
        assertFalse(hit(330f, 1000f, r = 0f))
        assertTrue(hit(330f, 1000f, r = 30f))
        assertFalse(hit(100f, 1000f, r = 30f))
    }

    @Test fun hitTestFollowsZoomAndPan() {
        // Con zoom 2 el objeto ocupa x de 200 a 800 (centro 500). Un punto en x = 250 ahora es objeto:
        assertTrue(hit(250f, 1000f, zoom = 2f))
        assertFalse(hit(250f, 1000f, zoom = 1f))
        // Con el objeto desplazado 300 px a la derecha, el centro del objeto está en x = 800:
        assertTrue(hit(800f, 1000f, panX = 300f))
        assertFalse(hit(500f, 1000f, zoom = 0.2f, panX = 300f))
    }

    @Test fun pinchKeepsThePointUnderTheFingers() {
        val cx = 500f
        val cy = 1000f
        // Dedos quietos en (700, 1400), zoom x2: el punto que estaba bajo los dedos sigue ahí.
        val s = applyPinch(1f, 0f, 0f, 2f, 700f, 1400f, 700f, 1400f, cx, cy, 0.2f, 8f)
        assertEquals(2f, s.zoom, 0.0001f)
        // punto_sin_zoom = c + (p - c - pan)/zoom → debe ser el mismo antes y después.
        val before = cx + (700f - cx - 0f) / 1f
        val after = cx + (700f - cx - s.panX) / s.zoom
        assertEquals(before, after, 0.01f)
    }

    @Test fun pinchRespectsZoomLimits() {
        assertEquals(8f, applyPinch(4f, 0f, 0f, 10f, 0f, 0f, 0f, 0f, 500f, 1000f, 0.2f, 8f).zoom, 0.0001f)
        assertEquals(0.2f, applyPinch(0.5f, 0f, 0f, 0.01f, 0f, 0f, 0f, 0f, 500f, 1000f, 0.2f, 8f).zoom, 0.0001f)
    }

    @Test fun pinchPanFollowsTheFingers() {
        val s = applyPinch(1f, 0f, 0f, 1f, 500f, 1000f, 560f, 1030f, 500f, 1000f, 0.2f, 8f)
        assertEquals(60f, s.panX, 0.01f)
        assertEquals(30f, s.panY, 0.01f)
    }

    @Test fun exitDistanceDependsOnWhereTheImageIs() {
        // Imagen pequeña (zoom bajo) en el centro: sale con poco recorrido.
        val small = ImageRect(450f, 950f, 550f, 1050f)
        val t = assertNotNull(computeExitTargetForRect(100f, 0f, small, sw, sh, 10f))
        assertTrue(t.dx >= 550f && t.dx < 700f, "dx = ${t.dx}")
        // Imagen mayor que la pantalla (zoom alto): necesita más recorrido que la pantalla.
        val big = ImageRect(-1000f, -2000f, 2000f, 4000f)
        val tb = assertNotNull(computeExitTargetForRect(100f, 0f, big, sw, sh, 10f))
        assertTrue(tb.dx >= 2000f)
        // En todos los casos el rectángulo queda fuera de la pantalla.
        assertTrue(small.left + t.dx >= sw)
        assertTrue(abs(t.dy) < 0.001f)
    }
}
