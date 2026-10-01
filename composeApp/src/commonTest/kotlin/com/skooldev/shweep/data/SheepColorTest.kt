package com.skooldev.shweep.data

import com.skooldev.shweep.screens.SheepArtwork
import kotlin.test.Test
import kotlin.test.assertEquals

class SheepColorTest {

    @Test
    fun whiteColorMapsToWhiteArtwork() {
        assertEquals(SheepArtwork.WHITE, SheepColor.WHITE.toArtwork())
    }

    @Test
    fun blackColorMapsToBlackArtwork() {
        assertEquals(SheepArtwork.BLACK, SheepColor.BLACK.toArtwork())
    }

    @Test
    fun fromStorageReturnsWhiteForNull() {
        assertEquals(SheepColor.WHITE, SheepColor.fromStorage(null))
    }

    @Test
    fun fromStorageReturnsWhiteForEmpty() {
        assertEquals(SheepColor.WHITE, SheepColor.fromStorage(""))
    }

    @Test
    fun fromStorageReturnsWhiteForUnknown() {
        assertEquals(SheepColor.WHITE, SheepColor.fromStorage("purple"))
    }

    @Test
    fun fromStorageReturnsWhiteForWhite() {
        assertEquals(SheepColor.WHITE, SheepColor.fromStorage("white"))
    }

    @Test
    fun fromStorageReturnsBlackForBlack() {
        assertEquals(SheepColor.BLACK, SheepColor.fromStorage("black"))
    }

    @Test
    fun storageValuesAreLowerCase() {
        assertEquals("white", SheepColor.WHITE.storageValue)
        assertEquals("black", SheepColor.BLACK.storageValue)
        assertEquals("colorful", SheepColor.COLORFUL.storageValue)
    }

    @Test
    fun colorfulColorMapsToColorfulArtwork() {
        assertEquals(SheepArtwork.COLORFUL, SheepColor.COLORFUL.toArtwork())
    }

    @Test
    fun fromStorageReturnsColorfulForColorful() {
        assertEquals(SheepColor.COLORFUL, SheepColor.fromStorage("colorful"))
    }

    @Test
    fun effectiveColorFallsBackToWhiteWithoutEntitlement() {
        assertEquals(
            SheepColor.WHITE,
            effectiveSheepColor(SheepColor.COLORFUL, hasColorfulSheep = false)
        )
    }

    @Test
    fun effectiveColorKeepsColorfulWithEntitlement() {
        assertEquals(
            SheepColor.COLORFUL,
            effectiveSheepColor(SheepColor.COLORFUL, hasColorfulSheep = true)
        )
    }

    @Test
    fun effectiveColorLeavesFreeColorsUnchanged() {
        assertEquals(
            SheepColor.BLACK,
            effectiveSheepColor(SheepColor.BLACK, hasColorfulSheep = false)
        )
        assertEquals(
            SheepColor.WHITE,
            effectiveSheepColor(SheepColor.WHITE, hasColorfulSheep = false)
        )
    }
}
