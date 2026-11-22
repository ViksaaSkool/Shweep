package com.skooldev.shweep.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SheepPressTest {

    private val size = 160f

    @Test
    fun returnsTopmostSheepWhenSheepOverlap() {
        val sheep = listOf(
            SheepItem(id = 1, x = 200f, y = 400f, vx = 0f, vy = 0f),
            SheepItem(id = 2, x = 220f, y = 420f, vx = 0f, vy = 0f)
        )

        assertEquals(2, findSheepIdAt(sheep, x = 260f, y = 460f, sheepBaseSizePx = size))
    }

    @Test
    fun acceptsAThumbJustOutsideTheRenderedBitmap() {
        val sheep = listOf(SheepItem(id = 7, x = 100f, y = 500f, vx = 0f, vy = 0f))

        assertEquals(
            7,
            findSheepIdAt(sheep, x = 100f - size * 0.09f, y = 500f, sheepBaseSizePx = size)
        )
    }

    @Test
    fun rejectsPointsBeyondTheHitbox() {
        val sheep = listOf(SheepItem(id = 7, x = 100f, y = 500f, vx = 0f, vy = 0f))

        assertNull(
            findSheepIdAt(sheep, x = 100f - size * 0.2f, y = 500f, sheepBaseSizePx = size)
        )
        assertNull(findSheepIdAt(sheep, x = 1100f, y = 500f, sheepBaseSizePx = size))
    }

    @Test
    fun returnsNullForEmptyMeadowOrUnknownSize() {
        assertNull(findSheepIdAt(emptyList(), x = 10f, y = 10f, sheepBaseSizePx = size))
        assertNull(
            findSheepIdAt(
                listOf(SheepItem(id = 1, x = 0f, y = 0f, vx = 0f, vy = 0f)),
                x = 0f,
                y = 0f,
                sheepBaseSizePx = 0f
            )
        )
    }

    @Test
    fun longPressRequiresATargetNoMovementAndElapsedTimeout() {
        assertTrue(
            isSheepLongPressTriggered(
                hasTarget = true,
                distancePx = 4f,
                touchSlopPx = 8f,
                elapsedMillis = 500L,
                timeoutMillis = 500L
            )
        )
        assertFalse(
            isSheepLongPressTriggered(
                hasTarget = false,
                distancePx = 4f,
                touchSlopPx = 8f,
                elapsedMillis = 500L,
                timeoutMillis = 500L
            )
        )
        assertFalse(
            isSheepLongPressTriggered(
                hasTarget = true,
                distancePx = 9f,
                touchSlopPx = 8f,
                elapsedMillis = 500L,
                timeoutMillis = 500L
            )
        )
        assertFalse(
            isSheepLongPressTriggered(
                hasTarget = true,
                distancePx = 4f,
                touchSlopPx = 8f,
                elapsedMillis = 499L,
                timeoutMillis = 500L
            )
        )
    }
}
