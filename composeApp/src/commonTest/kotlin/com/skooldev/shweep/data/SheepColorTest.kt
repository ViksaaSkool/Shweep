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
    }
}
