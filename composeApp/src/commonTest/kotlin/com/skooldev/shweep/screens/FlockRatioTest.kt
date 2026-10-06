package com.skooldev.shweep.screens

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The mini flock shows one sheep per [SHEEP_PER_ICON] counted sheep, capped at
 * [MAX_FLOCK_ICONS]. These tests pin the ratio, the cap, and the overflow
 * behaviour that keeps a long session from looking identical to a short one.
 */
class FlockRatioTest {

    @Test
    fun zeroSheepShowsNoIcons() {
        assertEquals(0, flockIconCount(0))
    }

    @Test
    fun oneToTenSheepShowsOneIcon() {
        assertEquals(1, flockIconCount(1))
        assertEquals(1, flockIconCount(10))
    }

    @Test
    fun iconCountRoundsUpToTheNextRatioStep() {
        assertEquals(2, flockIconCount(11))
        assertEquals(2, flockIconCount(20))
        assertEquals(3, flockIconCount(21))
        assertEquals(4, flockIconCount(31))
        assertEquals(5, flockIconCount(41))
    }

    @Test
    fun iconCountIsCappedAtTheMaximum() {
        assertEquals(MAX_FLOCK_ICONS, flockIconCount(50))
        assertEquals(MAX_FLOCK_ICONS, flockIconCount(500))
    }

    @Test
    fun iconCountNeverIncreasesPastTheCap() {
        var previous = 0
        for (count in 0..600) {
            val icons = flockIconCount(count)
            assertEquals(true, icons >= previous, "icons must not shrink at $count")
            previous = icons
        }
    }

    @Test
    fun noOverflowUpToTheCap() {
        assertEquals(0, flockOverflowCount(0))
        assertEquals(0, flockOverflowCount(35))
        assertEquals(0, flockOverflowCount(50))
    }

    @Test
    fun overflowCountsSheepBeyondTheCap() {
        assertEquals(1, flockOverflowCount(51))
        assertEquals(10, flockOverflowCount(60))
        assertEquals(450, flockOverflowCount(500))
    }

    @Test
    fun iconsAndOverflowAlwaysAccountForEverySheep() {
        for (count in 0..600) {
            val represented = flockIconCount(count) * SHEEP_PER_ICON + flockOverflowCount(count)
            assertEquals(
                true,
                represented >= count,
                "$count sheep under-represented at $count (represented $represented)"
            )
            assertEquals(
                true,
                represented - count < SHEEP_PER_ICON,
                "$count sheep over-represented at $count (represented $represented)"
            )
        }
    }
}