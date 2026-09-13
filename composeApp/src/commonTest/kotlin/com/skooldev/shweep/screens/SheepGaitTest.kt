package com.skooldev.shweep.screens

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheepGaitTest {

    private val baseSizePx = 160f
    private val tau = SheepGait.TAU

    @Test
    fun zeroSpeedLeavesPhaseUnchanged() {
        val phase = 1.7f
        assertEquals(
            phase,
            SheepGait.advancePhase(phase, speedPxPerSecond = 0f, deltaSeconds = 1f / 60f, sheepBaseSizePx = baseSizePx)
        )
    }

    @Test
    fun knownSpeedProducesExpectedPhase() {
        // One full cycle per stride: distance == stride length -> TAU,
        // which wraps back to (approximately) zero.
        val stride = baseSizePx * SheepGait.STRIDE_FACTOR
        val result = SheepGait.advancePhase(
            currentPhaseRadians = 0f,
            speedPxPerSecond = stride,
            deltaSeconds = 1f,
            sheepBaseSizePx = baseSizePx
        )
        assertTrue(result < 1e-3f, "expected wrapped phase ~0, got $result")
    }

    @Test
    fun phaseAdvancementIsFrameRateIndependent() {
        val speed = 200f

        fun run(steps: Int, dt: Float): Float {
            var phase = 0.4f
            repeat(steps) {
                phase = SheepGait.advancePhase(phase, speed, dt, baseSizePx)
            }
            return phase
        }

        val at30Hz = run(30, 1f / 30f)
        val at60Hz = run(60, 1f / 60f)
        val at120Hz = run(120, 1f / 120f)

        assertEquals(at60Hz, at30Hz, absoluteTolerance = 1e-3f)
        assertEquals(at60Hz, at120Hz, absoluteTolerance = 1e-3f)
    }

    @Test
    fun directionDoesNotAffectAdvancement() {
        val speed = 150f
        val dt = 1f / 60f

        val horizontal = SheepGait.advancePhase(0f, SheepGait.speed(speed, 0f), dt, baseSizePx)
        val vertical = SheepGait.advancePhase(0f, SheepGait.speed(0f, speed), dt, baseSizePx)
        val diagonal = SheepGait.advancePhase(0f, SheepGait.speed(speed / sqrt2, speed / sqrt2), dt, baseSizePx)

        assertEquals(horizontal, vertical, absoluteTolerance = 1e-6f)
        assertEquals(horizontal, diagonal, absoluteTolerance = 1e-6f)
    }

    @Test
    fun phaseAlwaysWrapsIntoRange() {
        var phase = tau - 0.01f
        repeat(100) {
            phase = SheepGait.advancePhase(phase, 400f, 1f / 60f, baseSizePx)
            assertTrue(phase >= 0f && phase < tau, "phase out of range: $phase")
            assertFalse(phase.isNaN())
        }
    }

    @Test
    fun fastMovementIsCappedAtMaxCadence() {
        val maxStep = SheepGait.TAU * SheepGait.MAX_GAIT_HZ * (1f / 60f)
        val result = SheepGait.advancePhase(
            0f,
            speedPxPerSecond = 100_000f,
            deltaSeconds = 1f / 60f,
            sheepBaseSizePx = baseSizePx
        )
        assertEquals(maxStep, result, absoluteTolerance = 1e-5f)
    }

    @Test
    fun diagonalPairsSharePhaseAndOpposingPairsAreAntiphase() {
        val nearRatio = SheepGait.NEAR_LEG_AMPLITUDE_DEGREES / SheepGait.FAR_LEG_AMPLITUDE_DEGREES
        val pose = SheepGait.pose(phaseRadians = 1.1f, speedPxPerSecond = 250f)

        // Same-phase diagonal pairs, scaled only by their amplitudes.
        assertEquals(
            pose.frontNearAngleDegrees,
            pose.rearFarAngleDegrees * nearRatio,
            absoluteTolerance = 1e-4f
        )
        assertEquals(
            pose.frontFarAngleDegrees,
            pose.rearNearAngleDegrees / nearRatio,
            absoluteTolerance = 1e-4f
        )
        // Opposite legs move in exact antiphase.
        assertEquals(pose.frontNearAngleDegrees, -pose.rearNearAngleDegrees, absoluteTolerance = 1e-5f)
        assertEquals(pose.frontFarAngleDegrees, -pose.rearFarAngleDegrees, absoluteTolerance = 1e-5f)
    }

    @Test
    fun anglesStayWithinConfiguredLimits() {
        for (step in 0 until 64) {
            val pose = SheepGait.pose(step * tau / 64f, speedPxPerSecond = 10_000f)
            assertTrue(abs(pose.frontNearAngleDegrees) <= SheepGait.NEAR_LEG_AMPLITUDE_DEGREES + 1e-4f)
            assertTrue(abs(pose.rearNearAngleDegrees) <= SheepGait.NEAR_LEG_AMPLITUDE_DEGREES + 1e-4f)
            assertTrue(abs(pose.frontFarAngleDegrees) <= SheepGait.FAR_LEG_AMPLITUDE_DEGREES + 1e-4f)
            assertTrue(abs(pose.rearFarAngleDegrees) <= SheepGait.FAR_LEG_AMPLITUDE_DEGREES + 1e-4f)
        }
    }

    @Test
    fun stationaryPoseIsNeutral() {
        val pose = SheepGait.pose(1.234f, speedPxPerSecond = 0f)
        assertEquals(0f, pose.frontNearAngleDegrees)
        assertEquals(0f, pose.frontFarAngleDegrees)
        assertEquals(0f, pose.rearNearAngleDegrees)
        assertEquals(0f, pose.rearFarAngleDegrees)
        assertEquals(0f, pose.bodyBobSourceUnits)
    }

    @Test
    fun bodyBobStaysBelowLimit() {
        for (step in 0 until 64) {
            val pose = SheepGait.pose(step * tau / 64f, speedPxPerSecond = 500f)
            assertTrue(pose.bodyBobSourceUnits >= 0f)
            assertTrue(
                pose.bodyBobSourceUnits <= SheepGait.BODY_BOB_SOURCE_UNITS + 1e-4f,
                "bob too large: ${pose.bodyBobSourceUnits}"
            )
        }
    }

    @Test
    fun whiteAndBlackRigsUseIdenticalGeometry() {
        val white = SHEEP_RIG_GEOMETRY
        // There is a single shared geometry rig; this asserts the slots and
        // draw order are complete and stable.
        assertEquals(4, white.legDrawOrder.size)
        assertEquals(setOf(LegSlot.FRONT_NEAR, LegSlot.FRONT_FAR, LegSlot.REAR_NEAR, LegSlot.REAR_FAR), white.legDrawOrder.toSet())
        assertTrue(white.body.pivotSourceX >= 0f && white.body.pivotSourceY >= 0f)
        for (slot in LegSlot.entries) {
            val g = white[slot]
            assertTrue(g.pivotSourceY > g.bitmapOffsetY, "$slot pivot must sit inside its crop")
        }
    }

    private companion object {
        val sqrt2 = kotlin.math.sqrt(2f)
    }
}
