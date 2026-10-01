package com.skooldev.shweep.data

import com.skooldev.shweep.screens.SheepArtwork

enum class SheepColor(val storageValue: String) {
    WHITE("white"),
    BLACK("black"),
    COLORFUL("colorful");

    companion object {
        fun fromStorage(value: String?): SheepColor =
            entries.firstOrNull { it.storageValue == value } ?: WHITE
    }
}

fun SheepColor.toArtwork(): SheepArtwork = when (this) {
    SheepColor.WHITE -> SheepArtwork.WHITE
    SheepColor.BLACK -> SheepArtwork.BLACK
    SheepColor.COLORFUL -> SheepArtwork.COLORFUL
}

/**
 * The color actually rendered. Colorful sheep are a paid feature, so a stored [SheepColor.COLORFUL]
 * preference falls back to [SheepColor.WHITE] while the `colorful_sheep` entitlement is absent.
 * The stored preference is kept, so it re-applies if the entitlement returns.
 */
fun effectiveSheepColor(selected: SheepColor, hasColorfulSheep: Boolean): SheepColor =
    if (selected == SheepColor.COLORFUL && !hasColorfulSheep) SheepColor.WHITE else selected
