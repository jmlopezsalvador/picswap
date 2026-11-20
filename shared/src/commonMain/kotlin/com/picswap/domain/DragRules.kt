package com.picswap.domain

import kotlin.math.hypot
import kotlin.math.min

/** Cómo se comporta el arrastre del objeto en el visor. */
enum class DragMode {
    /** Arrastrar y soltar: al soltar, el objeto desaparece (en cualquier sitio o solo junto al borde). */
    Drop,

    /** Acompañar: como pasar de foto en una galería. Sigue el dedo y se va si el gesto es suficiente; si no, vuelve. */
    Follow,
}

/**
 * @param edgeOnly (solo en modo `Drop`) el objeto solo desaparece si se suelta dentro de un marco pegado a los
 *   bordes de la pantalla; si se suelta en otro sitio, vuelve a su posición.
 * @param edgeDp ancho de ese marco, en dp.
 */
data class DragSettings(
    val mode: DragMode = DragMode.Drop,
    val edgeOnly: Boolean = false,
    val edgeDp: Int = 80,
)

/** ¿Está el punto (x, y) dentro del marco de ancho `band` pegado a los bordes de una pantalla `width` × `height`? */
fun isInEdgeBand(x: Float, y: Float, width: Float, height: Float, band: Float): Boolean =
    x <= band || y <= band || x >= width - band || y >= height - band

/**
 * Modo «acompañar»: ¿se completa la salida al soltar?
 * Sí si se ha arrastrado un cuarto del lado menor de la pantalla, o si el gesto es un lanzamiento rápido
 * (velocidad ≥ `minSpeed`) en el mismo sentido del arrastre.
 */
fun shouldCommitFollow(
    dragX: Float,
    dragY: Float,
    velX: Float,
    velY: Float,
    width: Float,
    height: Float,
    minSpeed: Float,
): Boolean {
    val dist = hypot(dragX, dragY)
    if (dist >= 0.25f * min(width, height)) return true
    val speed = hypot(velX, velY)
    val sameDirection = dragX * velX + dragY * velY > 0f
    return dist > 1f && speed >= minSpeed && sameDirection
}
