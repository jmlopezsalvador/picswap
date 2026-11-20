package com.picswap.domain

import kotlin.math.abs
import kotlin.math.hypot

/** Resultado del cálculo de salida: desplazamiento final (px) que deja la imagen totalmente fuera de la pantalla. */
data class ExitTarget(val dx: Float, val dy: Float)

/**
 * Calcula hacia dónde debe terminar de salir la imagen cuando el usuario suelta el dedo.
 *
 * La dirección es la del arrastre acumulado (`dragX`, `dragY`), en cualquier ángulo. La distancia es la mínima
 * para que la imagen (que ocupa como mucho `width` × `height`) quede completamente fuera de la pantalla.
 *
 * Devuelve `null` si el arrastre es demasiado corto (`minDrag`): en ese caso la imagen vuelve a su sitio.
 */
fun computeExitTarget(dragX: Float, dragY: Float, width: Float, height: Float, minDrag: Float): ExitTarget? {
    val length = hypot(dragX, dragY)
    if (length < minDrag || length == 0f) return null
    val ux = dragX / length
    val uy = dragY / length
    // Sale por el eje que antes quede fuera; el margen del 2 % evita que asome un píxel por redondeo.
    val distX = if (abs(ux) < 1e-6f) Float.MAX_VALUE else width / abs(ux)
    val distY = if (abs(uy) < 1e-6f) Float.MAX_VALUE else height / abs(uy)
    val distance = minOf(distX, distY) * 1.02f
    return ExitTarget(ux * distance, uy * distance)
}
