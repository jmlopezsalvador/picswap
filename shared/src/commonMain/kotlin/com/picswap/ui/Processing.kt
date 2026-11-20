package com.picswap.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.picswap.domain.cutoutBackground

/**
 * Ajustes de «quitar fondo» de una foto.
 * @param color color plano (ARGB) que se verá detrás del objeto.
 * @param tolerance 1-100.
 * @param refColor color a retirar elegido con el cuentagotas (null = se detecta con los bordes).
 * @param seedX posición relativa (0-1) tocada con el cuentagotas; junto con `seedY`.
 */
data class EditParams(
    val color: Long,
    val tolerance: Int,
    val refColor: Int? = null,
    val seedX: Float? = null,
    val seedY: Float? = null,
)

/** Lee los píxeles ARGB de una imagen. */
fun pixelsOf(image: ImageBitmap): IntArray {
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels, 0, 0, image.width, image.height)
    return pixels
}

/** Aplica el recorte a unos píxeles (el fondo pasa a ser transparente). Trabajo pesado: hilo de fondo. */
fun applyCutout(pixels: IntArray, width: Int, height: Int, params: EditParams): IntArray {
    val seed = if (params.seedX != null && params.seedY != null) {
        val x = (params.seedX * width).toInt().coerceIn(0, width - 1)
        val y = (params.seedY * height).toInt().coerceIn(0, height - 1)
        y * width + x
    } else null
    return cutoutBackground(pixels, width, height, params.tolerance, params.refColor, seed)
}

/** Recorte de una imagen completa codificado como PNG con transparencia. Null si falla. */
fun makeCutoutPng(image: ImageBitmap, params: EditParams): ByteArray? = try {
    val cut = applyCutout(pixelsOf(image), image.width, image.height, params)
    encodePng(image.width, image.height, cut).takeIf { it.isNotEmpty() }
} catch (e: Exception) {
    null
}
