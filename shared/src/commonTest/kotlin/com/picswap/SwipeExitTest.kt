package com.picswap

import com.picswap.domain.computeExitTarget
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeExitTest {
    @Test fun shortDragReturnsNull() {
        assertNull(computeExitTarget(3f, 4f, 1000f, 2000f, minDrag = 20f))
    }

    @Test fun horizontalDragExitsThroughTheSide() {
        val t = assertNotNull(computeExitTarget(-100f, 0f, 1000f, 2000f, 20f))
        assertTrue(t.dx <= -1000f)
        assertEquals(0f, t.dy, 0.001f)
    }

    @Test fun verticalDragExitsThroughTopOrBottom() {
        val t = assertNotNull(computeExitTarget(0f, 80f, 1000f, 2000f, 20f))
        assertTrue(t.dy >= 2000f)
        assertEquals(0f, t.dx, 0.001f)
    }

    @Test fun diagonalDragLeavesScreenCompletely() {
        val t = assertNotNull(computeExitTarget(60f, 90f, 1000f, 2000f, 20f))
        // Fuera por al menos un eje y conservando la dirección del arrastre.
        assertTrue(abs(t.dx) >= 1000f || abs(t.dy) >= 2000f)
        assertEquals(60f / 90f, t.dx / t.dy, 0.001f)
    }
}
