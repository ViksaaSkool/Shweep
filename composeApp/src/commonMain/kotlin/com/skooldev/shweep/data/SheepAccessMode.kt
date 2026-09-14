package com.skooldev.shweep.data

enum class SheepAccessMode {
    UNLIMITED,
    LIMITED
}

fun resolveSheepAccessMode(
    limitedSheepEnabled: Boolean
): SheepAccessMode {
    if (!limitedSheepEnabled) return SheepAccessMode.UNLIMITED

    return SheepAccessMode.LIMITED
}
