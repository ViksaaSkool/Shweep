package com.skooldev.shweep.screens

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure, frame-rate independent gait math.
 *
 * Gait phase advances with distance traveled rather than wall-clock time so
 * faster sheep step faster and the animation stays consistent on any refresh
 * rate. All angles are in degrees for direct use with DrawScope.rotate.
 */
internal object SheepGait {

    const val TAU = (2 * PI).toFloat()

    /** Stride length as a fraction of the sheep base size in pixels. */
    const val STRIDE_FACTOR = 0.60f

    /** Upper bound on gait cycles per second; prevents tiny-sheep vibration. */
    const val MAX_GAIT_HZ = 4f

    /** Speed in px/s that maps to full swing amplitude. */
    const val SPEED_REFERENCE_PX_PER_SECOND = 250f

    /** Swing amplitude in degrees: near legs are more visible than far legs. */
    const val NEAR_LEG_AMPLITUDE_DEGREES = 25f
    const val FAR_LEG_AMPLITUDE_DEGREES = 18f

    /** Vertical body bob amplitude at full speed, in source artwork units. */
    const val BODY_BOB_SOURCE_UNITS = 18f

    /**
     * Advance a gait phase for one simulation step.
     *
     * [deltaSeconds] must already be clamped by the caller to the simulation
     * maximum. The per-step advance is additionally capped so that very fast
     * sheep cannot alias into jitter.
     */
    fun advancePhase(
        currentPhaseRadians: Float,
        speedPxPerSecond: Float,
        deltaSeconds: Float,
        sheepBaseSizePx: Float
    ): Float {
        if (speedPxPerSecond <= 0f || deltaSeconds <= 0f || sheepBaseSizePx <= 0f) {
            return currentPhaseRadians
        }
        val strideLengthPx = sheepBaseSizePx * STRIDE_FACTOR
        val distancePx = speedPxPerSecond * deltaSeconds
        var delta = TAU * distancePx / strideLengthPx

        val maxDelta = TAU * MAX_GAIT_HZ * deltaSeconds
        if (delta > maxDelta) delta = maxDelta

        return positiveModulo(currentPhaseRadians + delta, TAU)
    }

    /**
     * Map a gait phase and speed to leg rotation angles and body bob.
     *
     * Diagonal pairs move together (trot): front-near + rear-far share one
     * phase, front-far + rear-near run in exact antiphase. Amplitude fades to
     * zero as speed approaches zero so stationary sheep stand still. The bob
     * lifts the entire rig (body plus legs) to avoid seams.
     */
    fun pose(phaseRadians: Float, speedPxPerSecond: Float): SheepGaitPose {
        val speedFactor = (speedPxPerSecond / SPEED_REFERENCE_PX_PER_SECOND)
            .coerceIn(0f, 1f)

        // Neutral stance at rest; avoids -0.0 artifacts as well.
        if (speedFactor <= 0f) {
            return SheepGaitPose(
                frontNearAngleDegrees = 0f,
                frontFarAngleDegrees = 0f,
                rearNearAngleDegrees = 0f,
                rearFarAngleDegrees = 0f,
                bodyBobSourceUnits = 0f
            )
        }

        val diagonalA = sin(phaseRadians) * speedFactor
        val diagonalB = -diagonalA

        val bob = abs(sin(phaseRadians)) * BODY_BOB_SOURCE_UNITS * speedFactor

        return SheepGaitPose(
            frontNearAngleDegrees = NEAR_LEG_AMPLITUDE_DEGREES * diagonalA,
            rearFarAngleDegrees = FAR_LEG_AMPLITUDE_DEGREES * diagonalA,
            frontFarAngleDegrees = FAR_LEG_AMPLITUDE_DEGREES * diagonalB,
            rearNearAngleDegrees = NEAR_LEG_AMPLITUDE_DEGREES * diagonalB,
            bodyBobSourceUnits = bob
        )
    }

    fun positiveModulo(value: Float, range: Float): Float {
        val remainder = value % range
        return if (remainder >= 0f) remainder else remainder + range
    }

    /** Convenience speed magnitude used by both simulation and renderer. */
    fun speed(vx: Float, vy: Float): Float = sqrt(vx * vx + vy * vy)
}

internal data class SheepGaitPose(
    val frontNearAngleDegrees: Float,
    val frontFarAngleDegrees: Float,
    val rearNearAngleDegrees: Float,
    val rearFarAngleDegrees: Float,
    val bodyBobSourceUnits: Float
)
