package com.skooldev.shweep.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.compose.resources.imageResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.sheep_black_body
import shweep.composeapp.generated.resources.sheep_black_front_far_leg
import shweep.composeapp.generated.resources.sheep_black_front_near_leg
import shweep.composeapp.generated.resources.sheep_black_rear_far_leg
import shweep.composeapp.generated.resources.sheep_black_rear_near_leg
import shweep.composeapp.generated.resources.sheep_white_body
import shweep.composeapp.generated.resources.sheep_white_front_far_leg
import shweep.composeapp.generated.resources.sheep_white_front_near_leg
import shweep.composeapp.generated.resources.sheep_white_rear_far_leg
import shweep.composeapp.generated.resources.sheep_white_rear_near_leg
import kotlin.math.roundToInt

internal data class SheepRigImages(
    val body: ImageBitmap,
    val frontNear: ImageBitmap,
    val frontFar: ImageBitmap,
    val rearNear: ImageBitmap,
    val rearFar: ImageBitmap
) {
    operator fun get(slot: LegSlot): ImageBitmap = when (slot) {
        LegSlot.FRONT_NEAR -> frontNear
        LegSlot.FRONT_FAR -> frontFar
        LegSlot.REAR_NEAR -> rearNear
        LegSlot.REAR_FAR -> rearFar
    }
}

@Composable
internal fun rememberWhiteSheepRigImages(): SheepRigImages = SheepRigImages(
    body = imageResource(Res.drawable.sheep_white_body),
    frontNear = imageResource(Res.drawable.sheep_white_front_near_leg),
    frontFar = imageResource(Res.drawable.sheep_white_front_far_leg),
    rearNear = imageResource(Res.drawable.sheep_white_rear_near_leg),
    rearFar = imageResource(Res.drawable.sheep_white_rear_far_leg)
)

@Composable
internal fun rememberBlackSheepRigImages(): SheepRigImages = SheepRigImages(
    body = imageResource(Res.drawable.sheep_black_body),
    frontNear = imageResource(Res.drawable.sheep_black_front_near_leg),
    frontFar = imageResource(Res.drawable.sheep_black_front_far_leg),
    rearNear = imageResource(Res.drawable.sheep_black_rear_near_leg),
    rearFar = imageResource(Res.drawable.sheep_black_rear_far_leg)
)

/**
 * Draws the whole flock on a single canvas.
 *
 * Each sheep costs five bitmap draws (four rotated legs behind one body) and
 * zero extra Compose layout nodes, keeping composition cheap at high counts.
 * Leg angles come straight from the simulation's gait phase, so no per-sheep
 * animation objects or coroutines exist.
 *
 * [frameCounter] forces a recomposition every frame so the draw lambda
 * always sees the latest gait phase, even when position hasn't changed.
 */
@Composable
internal fun LayeredSheepCanvas(
    sheepList: List<SheepItem>,
    sheepBaseSizePx: Float,
    frameCounter: Int = 0,
    modifier: Modifier = Modifier
) {
    val whiteImages = rememberWhiteSheepRigImages()
    val hasBlackSheep = sheepList.any { it.artwork == SheepArtwork.BLACK }
    val blackImages = if (hasBlackSheep) rememberBlackSheepRigImages() else null

    Canvas(modifier = modifier) {
        for (sheep in sheepList) {
            val images = when (sheep.artwork) {
                SheepArtwork.WHITE -> whiteImages
                SheepArtwork.BLACK -> blackImages ?: continue
            }

            val pose = SheepGait.pose(
                phaseRadians = sheep.gaitPhaseRadians,
                speedPxPerSecond = SheepGait.speed(sheep.vx, sheep.vy)
            )

            // Maps source artwork units to current on-screen pixels.
            val unitScale = sheepBaseSizePx / 1024f

            withTransform({
                translate(left = sheep.x, top = sheep.y + pose.bodyBobSourceUnits * unitScale)
                scale(unitScale, unitScale, pivot = Offset.Zero)
            }) {
                for (slot in SHEEP_RIG_GEOMETRY.legDrawOrder) {
                    drawLeg(images[slot], SHEEP_RIG_GEOMETRY[slot], pose.angleFor(slot))
                }
                drawImage(
                    image = images.body,
                    dstOffset = IntOffset(
                        SHEEP_RIG_GEOMETRY.body.bitmapOffsetX.roundToInt(),
                        SHEEP_RIG_GEOMETRY.body.bitmapOffsetY.roundToInt()
                    )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLeg(
    bitmap: ImageBitmap,
    geometry: SheepLayerGeometry,
    angleDegrees: Float
) {
    withTransform({
        rotate(
            degrees = angleDegrees,
            pivot = Offset(geometry.pivotSourceX, geometry.pivotSourceY)
        )
    }) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(
                geometry.bitmapOffsetX.roundToInt(),
                geometry.bitmapOffsetY.roundToInt()
            )
        )
    }
}

private fun SheepGaitPose.angleFor(slot: LegSlot): Float = when (slot) {
    LegSlot.FRONT_NEAR -> frontNearAngleDegrees
    LegSlot.FRONT_FAR -> frontFarAngleDegrees
    LegSlot.REAR_NEAR -> rearNearAngleDegrees
    LegSlot.REAR_FAR -> rearFarAngleDegrees
}
