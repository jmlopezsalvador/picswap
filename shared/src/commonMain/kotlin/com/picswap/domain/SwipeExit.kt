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
fun computeExitTarget(dragX: Float, dragY: Float, width: Float, height: Float, minDrag: Float): ExitTarget? =
    computeExitTargetForRect(dragX, dragY, ImageRect(0f, 0f, width, height), width, height, minDrag)

/**
 * Igual que [computeExitTarget], pero para una imagen que ocupa el rectángulo `rect` (por ejemplo, con zoom o
 * desplazada): se desplaza lo justo, en la dirección del arrastre, para que el rectángulo quede completamente
 * fuera de la pantalla `screenW` × `screenH`.
 */
fun computeExitTargetForRect(
    dragX: Float,
    dragY: Float,
    rect: ImageRect,
    screenW: Float,
    screenH: Float,
    minDrag: Float,
): ExitTarget? {
    val length = hypot(dragX, dragY)
    if (length < minDrag || length == 0f) return null
    val ux = dragX / length
    val uy = dragY / length
    // Basta con salir por un eje. Si el rectángulo ya está fuera por ese eje, la distancia es 0.
    val distX = when {
        abs(ux) < 1e-6f -> Float.MAX_VALUE
        ux > 0f -> maxOf(0f, (screenW - rect.left) / ux)
        else -> maxOf(0f, rect.right / -ux)
    }
    val distY = when {
        abs(uy) < 1e-6f -> Float.MAX_VALUE
        uy > 0f -> maxOf(0f, (screenH - rect.top) / uy)
        else -> maxOf(0f, rect.bottom / -uy)
    }
    // El margen del 2 % (+1 px) evita que asome un píxel por redondeo.
    val distance = minOf(distX, distY) * 1.02f + 1f
    return ExitTarget(ux * distance, uy * distance)
}
