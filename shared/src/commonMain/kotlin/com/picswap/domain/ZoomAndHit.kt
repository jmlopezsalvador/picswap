package com.picswap.domain

import kotlin.math.min

/**
 * Geometría del visor. La imagen se dibuja «ajustada» (Fit) en una pantalla `width` × `height`, y encima se aplica
 * un zoom `zoom` alrededor del centro de la pantalla y un desplazamiento `pan` (px):
 *
 *     punto_en_pantalla = centro + pan + zoom * (punto_sin_zoom - centro)
 */

/** Rectángulo (px de pantalla) que ocupa la imagen entera. */
data class ImageRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Dónde queda en pantalla la imagen (`imgW` × `imgH` px) con el zoom y el desplazamiento dados. */
fun imageRectOnScreen(
    imgW: Int,
    imgH: Int,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
): ImageRect {
    val s0 = min(width / imgW, height / imgH)
    val left0 = (width - imgW * s0) / 2f
    val top0 = (height - imgH * s0) / 2f
    val cx = width / 2f
    val cy = height / 2f
    return ImageRect(
        left = cx + panX + zoom * (left0 - cx),
        top = cy + panY + zoom * (top0 - cy),
        right = cx + panX + zoom * (left0 + imgW * s0 - cx),
        bottom = cy + panY + zoom * (top0 + imgH * s0 - cy),
    )
}

/**
 * ¿Hay objeto visible (píxel no transparente) en el punto (x, y) de la pantalla, o a menos de `radius` px?
 * `alpha` es el canal alfa de la imagen (un byte por píxel, `imgW * imgH`). Se mira el punto y cuatro más a su
 * alrededor porque un dedo no es un punto: así tocar en el borde del objeto también cuenta.
 */
fun isOpaqueNear(
    alpha: ByteArray,
    imgW: Int,
    imgH: Int,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    radius: Float,
    threshold: Int = 128,
): Boolean {
    if (zoom <= 0f || imgW <= 0 || imgH <= 0) return false
    val s0 = min(width / imgW, height / imgH)
    val left0 = (width - imgW * s0) / 2f
    val top0 = (height - imgH * s0) / 2f
    val cx = width / 2f
    val cy = height / 2f

    fun opaqueAt(px: Float, py: Float): Boolean {
        val qx = cx + (px - cx - panX) / zoom
        val qy = cy + (py - cy - panY) / zoom
        val fx = (qx - left0) / s0
        val fy = (qy - top0) / s0
        if (fx < 0f || fy < 0f || fx >= imgW || fy >= imgH) return false
        return (alpha[fy.toInt() * imgW + fx.toInt()].toInt() and 0xFF) >= threshold
    }

    return opaqueAt(x, y) || opaqueAt(x + radius, y) || opaqueAt(x - radius, y) ||
        opaqueAt(x, y + radius) || opaqueAt(x, y - radius)
}

/** Zoom y desplazamiento resultantes de un gesto de pellizco. */
data class ZoomState(val zoom: Float, val panX: Float, val panY: Float)

/**
 * Aplica un cambio de pellizco manteniendo el punto que está bajo los dedos: `zoomChange` es la razón entre la
 * separación actual y la anterior de los dos dedos; (`oldCx`, `oldCy`) y (`newCx`, `newCy`) son el punto medio
 * anterior y el actual. El zoom se limita a [`minZoom`, `maxZoom`].
 */
fun applyPinch(
    zoom: Float,
    panX: Float,
    panY: Float,
    zoomChange: Float,
    oldCx: Float,
    oldCy: Float,
    newCx: Float,
    newCy: Float,
    centerX: Float,
    centerY: Float,
    minZoom: Float,
    maxZoom: Float,
): ZoomState {
    val newZoom = (zoom * zoomChange).coerceIn(minZoom, maxZoom)
    val ratio = newZoom / zoom
    return ZoomState(
        zoom = newZoom,
        panX = newCx - centerX - ratio * (oldCx - centerX - panX),
        panY = newCy - centerY - ratio * (oldCy - centerY - panY),
    )
}
