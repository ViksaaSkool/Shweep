package com.skooldev.shweep.data

import com.skooldev.shweep.screens.SheepArtwork

enum class SheepColor(val storageValue: String) {
    WHITE("white"),
    BLACK("black");

    companion object {
        fun fromStorage(value: String?): SheepColor =
            entries.firstOrNull { it.storageValue == value } ?: WHITE
    }
}

fun SheepColor.toArtwork(): SheepArtwork = when (this) {
    SheepColor.WHITE -> SheepArtwork.WHITE
    SheepColor.BLACK -> SheepArtwork.BLACK
}
