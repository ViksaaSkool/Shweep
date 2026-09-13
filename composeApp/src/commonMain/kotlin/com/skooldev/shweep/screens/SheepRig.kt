package com.skooldev.shweep.screens

/**
 * Static placement metadata for the layered sheep artwork.
 *
 * All coordinates live in the original 1024x1024 artwork space so pivots are
 * independent of crop sizes and device density. White and black variants use
 * identical geometry; only the bitmaps differ.
 */
internal data class SheepLayerGeometry(
    val bitmapOffsetX: Float,
    val bitmapOffsetY: Float,
    val pivotSourceX: Float,
    val pivotSourceY: Float
)

internal data class SheepRigGeometry(
    val body: SheepLayerGeometry,
    val frontNear: SheepLayerGeometry,
    val frontFar: SheepLayerGeometry,
    val rearNear: SheepLayerGeometry,
    val rearFar: SheepLayerGeometry
) {
    /** Draw order: far pair first, then near pair, all behind the body. */
    val legDrawOrder: List<LegSlot> = listOf(
        LegSlot.FRONT_FAR,
        LegSlot.REAR_FAR,
        LegSlot.FRONT_NEAR,
        LegSlot.REAR_NEAR
    )

    operator fun get(slot: LegSlot): SheepLayerGeometry = when (slot) {
        LegSlot.FRONT_NEAR -> frontNear
        LegSlot.FRONT_FAR -> frontFar
        LegSlot.REAR_NEAR -> rearNear
        LegSlot.REAR_FAR -> rearFar
    }
}

internal enum class LegSlot {
    FRONT_NEAR,
    FRONT_FAR,
    REAR_NEAR,
    REAR_FAR
}

internal val SHEEP_RIG_GEOMETRY = SheepRigGeometry(
    body = SheepLayerGeometry(
        bitmapOffsetX = 12f,
        bitmapOffsetY = 0f,
        pivotSourceX = 0f,
        pivotSourceY = 0f
    ),
    frontNear = SheepLayerGeometry(
        bitmapOffsetX = 254f,
        bitmapOffsetY = 620f,
        pivotSourceX = 337f,
        pivotSourceY = 705f
    ),
    frontFar = SheepLayerGeometry(
        bitmapOffsetX = 404f,
        bitmapOffsetY = 620f,
        pivotSourceX = 490f,
        pivotSourceY = 705f
    ),
    rearNear = SheepLayerGeometry(
        bitmapOffsetX = 679f,
        bitmapOffsetY = 620f,
        pivotSourceX = 762f,
        pivotSourceY = 705f
    ),
    rearFar = SheepLayerGeometry(
        bitmapOffsetX = 549f,
        bitmapOffsetY = 620f,
        pivotSourceX = 625f,
        pivotSourceY = 705f
    )
)
