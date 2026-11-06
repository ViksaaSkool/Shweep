package com.skooldev.shweep.screens

/**
 * Finds the topmost sheep under a pointer. Sheep are drawn in list order, so
 * the last matching entry is the one visually on top.
 *
 * The hitbox is intentionally slightly larger than the rendered bitmap so a
 * finger does not have to land on the sheep's face.
 */
internal fun findSheepIdAt(
    sheepList: List<SheepItem>,
    x: Float,
    y: Float,
    sheepBaseSizePx: Float,
    paddingFraction: Float = 0.1f
): Int? {
    if (sheepBaseSizePx <= 0f) return null

    val padding = sheepBaseSizePx * paddingFraction
    return sheepList.asReversed().firstOrNull { sheep ->
        x >= sheep.x - padding &&
            x <= sheep.x + sheepBaseSizePx + padding &&
            y >= sheep.y - padding &&
            y <= sheep.y + sheepBaseSizePx + padding
    }?.id
}

internal fun isSheepLongPressTriggered(
    hasTarget: Boolean,
    distancePx: Float,
    touchSlopPx: Float,
    elapsedMillis: Long,
    timeoutMillis: Long
): Boolean = hasTarget &&
    distancePx <= touchSlopPx &&
    elapsedMillis >= timeoutMillis
