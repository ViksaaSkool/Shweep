package com.skooldev.shweep.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheepSimulationTest {

    private val screenWidth = 1000f
    private val screenHeight = 2000f
    private val playAreaStartY = 700f
    private val baseSizePx = 160f

    private fun step(
        sheep: List<SheepItem>,
        deltaSeconds: Float,
        targetScale: Float = 1f
    ): List<SheepItem> = SheepSimulation.step(
        sheepList = sheep,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        playAreaStartY = playAreaStartY,
        sheepBaseSizePx = baseSizePx,
        targetScale = targetScale,
        deltaSeconds = deltaSeconds
    )

    @Test
    fun movementIsFrameRateIndependent() {
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 120f, vy = -60f))

        val at60Hz = runSteps(start, steps = 60, dt = 1f / 60f)
        val at120Hz = runSteps(start, steps = 120, dt = 1f / 120f)
        val at30Hz = runSteps(start, steps = 30, dt = 1f / 30f)

        assertEquals(at60Hz[0].x, at120Hz[0].x, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].y, at120Hz[0].y, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].x, at30Hz[0].x, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].y, at30Hz[0].y, absoluteTolerance = 0.01f)

        // One second of travel from the spawn point.
        assertEquals(520f, at60Hz[0].x, absoluteTolerance = 0.5f)
        assertEquals(840f, at60Hz[0].y, absoluteTolerance = 0.5f)
    }

    @Test
    fun boundaryCollisionReflectsOnlyAffectedAxis() {
        // Start one sub-step away from the right wall.
        val start = listOf(SheepItem(id = 0, x = screenWidth - baseSizePx - 1f, y = 900f, vx = 100f, vy = 50f))
        val result = step(start, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)

        assertEquals(screenWidth - baseSizePx, result[0].x, absoluteTolerance = 0.001f)
        assertTrue(result[0].vx < 0f)
        assertTrue(result[0].vy > 0f)
    }

    @Test
    fun topBoundaryClampsToPlayArea() {
        val start = listOf(SheepItem(id = 0, x = 500f, y = playAreaStartY - 50f, vx = 10f, vy = -100f))
        val result = step(start, deltaSeconds = 1f)

        assertEquals(playAreaStartY, result[0].y)
        assertTrue(result[0].vy > 0f)
    }

    @Test
    fun headOnCollisionExchangesNormalVelocity() {
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = 200f, vy = 0f)
        val b = SheepItem(id = 1, x = 440f + 1f, y = 900f, vx = -200f, vy = 0f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        // Normal points from a to b (+x): they exchange normal components.
        assertTrue(result[0].vx < 0f, "a should now move away leftward")
        assertTrue(result[1].vx > 0f, "b should now move away rightward")
    }

    @Test
    fun glancingCollisionPreservesTangentialVelocity() {
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = 200f, vy = 0f)
        val b = SheepItem(id = 1, x = 420f, y = 980f, vx = -200f, vy = 0f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        // A normal-only elastic impulse conserves total momentum and kinetic
        // energy regardless of the exact contact normal, while changing both
        // velocity directions (unlike the old full-vector swap, which would
        // also conserve these but produce unnatural zig-zags).
        val momentumBeforeX = a.vx + b.vx
        val momentumBeforeY = a.vy + b.vy
        val energyBefore = a.vx * a.vx + a.vy * a.vy + b.vx * b.vx + b.vy * b.vy

        val momentumAfterX = result[0].vx + result[1].vx
        val momentumAfterY = result[0].vy + result[1].vy
        val energyAfter =
            result[0].vx * result[0].vx + result[0].vy * result[0].vy +
                result[1].vx * result[1].vx + result[1].vy * result[1].vy

        assertEquals(momentumBeforeX, momentumAfterX, absoluteTolerance = 0.01f)
        assertEquals(momentumBeforeY, momentumAfterY, absoluteTolerance = 0.01f)
        assertEquals(energyBefore, energyAfter, absoluteTolerance = energyBefore * 0.001f)

        // Directions must actually change: this is a deflection, not a pass-through.
        val changedA = result[0].vx != a.vx || result[0].vy != a.vy
        val changedB = result[1].vx != b.vx || result[1].vy != b.vy
        assertTrue(changedA)
        assertTrue(changedB)
    }

    @Test
    fun separatingOverlapIsCorrectedWithoutImpulse() {
        // Overlapping but moving apart: positions separate, velocities unchanged.
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = -200f, vy = 0f)
        val b = SheepItem(id = 1, x = 380f, y = 900f, vx = 200f, vy = 0f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        assertEquals(-200f, result[0].vx)
        assertEquals(200f, result[1].vx)

        val centerDistance = kotlin.math.abs(
            (result[1].x + baseSizePx / 2f) - (result[0].x + baseSizePx / 2f)
        )
        assertTrue(centerDistance >= baseSizePx - 0.01f, "overlap was not resolved")
    }

    @Test
    fun identicalPositionsProduceFiniteResult() {
        val a = SheepItem(id = 0, x = 500f, y = 900f, vx = 100f, vy = 100f)
        val b = SheepItem(id = 1, x = 500f, y = 900f, vx = -100f, vy = -100f)
        // Zero delta keeps centers exactly identical to exercise the
        // degenerate-normal branch.
        val result = step(listOf(a, b), deltaSeconds = 0f)

        for (sheep in result) {
            assertFalse(sheep.x.isNaN())
            assertFalse(sheep.y.isNaN())
            assertFalse(sheep.vx.isNaN())
            assertFalse(sheep.vy.isNaN())
        }
        assertTrue(result[0].vx <= 0f)
        assertTrue(result[1].vx >= 0f)
    }

    @Test
    fun largeDeltaIsCapped() {
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 250f, vy = 0f))
        // 2 seconds requested; capped to MAX_DELTA_SECONDS = 0.05s.
        val expected = SheepSimulation.MAX_DELTA_SECONDS * 250f
        val result = step(start, deltaSeconds = 2f)

        assertEquals(400f + expected, result[0].x, absoluteTolerance = 0.001f)
    }

    @Test
    fun collisionSeparationStaysInsidePlayArea() {
        val a = SheepItem(id = 0, x = 0f, y = 900f, vx = 200f, vy = 0f)
        val b = SheepItem(id = 1, x = 100f, y = 900f, vx = -200f, vy = 0f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        for (sheep in result) {
            assertTrue(sheep.x >= 0f, "sheep pushed through left wall")
            assertTrue(sheep.x <= screenWidth - baseSizePx, "sheep pushed through right wall")
            assertTrue(sheep.y >= playAreaStartY)
            assertTrue(sheep.y <= screenHeight - baseSizePx)
        }
    }

    @Test
    fun targetScaleIsAppliedToAllSheep() {
        val start = listOf(
            SheepItem(id = 0, x = 100f, y = 800f, vx = 50f, vy = 0f),
            SheepItem(id = 1, x = 600f, y = 1200f, vx = -50f, vy = 0f)
        )
        val result = step(start, deltaSeconds = 1f / 60f, targetScale = 0.5f)

        for (sheep in result) {
            assertEquals(0.5f, sheep.scale)
        }
    }

    @Test
    fun gaitPhaseAdvancesWithMovement() {
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 250f, vy = 0f, gaitPhaseRadians = 0.5f))
        val expected = SheepGait.advancePhase(
            currentPhaseRadians = 0.5f,
            speedPxPerSecond = 250f,
            deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS,
            sheepBaseSizePx = baseSizePx
        )
        val result = step(start, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)
        assertEquals(expected, result[0].gaitPhaseRadians, absoluteTolerance = 1e-5f)
        assertTrue(result[0].gaitPhaseRadians > 0.5f)
    }

    @Test
    fun wallReflectionDoesNotReverseOrResetGaitPhase() {
        val nearWall = listOf(
            SheepItem(id = 0, x = screenWidth - baseSizePx - 1f, y = 900f, vx = 200f, vy = 0f, gaitPhaseRadians = 2.2f)
        )
        val result = step(nearWall, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)

        assertTrue(result[0].vx < 0f, "sheep should bounce off the wall")
        assertTrue(result[0].gaitPhaseRadians > 2.2f, "phase must keep advancing through the bounce")
        assertTrue(result[0].gaitPhaseRadians < SheepGait.TAU)
    }

    @Test
    fun collisionSeparationDoesNotAdvanceGaitPhase() {
        // Overlapping pair resolved with zero delta: phase must be untouched.
        val overlapping = listOf(
            SheepItem(id = 0, x = 300f, y = 900f, vx = -200f, vy = 0f, gaitPhaseRadians = 1.3f),
            SheepItem(id = 1, x = 380f, y = 900f, vx = 200f, vy = 0f, gaitPhaseRadians = 4.1f)
        )
        val result = step(overlapping, deltaSeconds = 0f)

        assertEquals(1.3f, result[0].gaitPhaseRadians)
        assertEquals(4.1f, result[1].gaitPhaseRadians)
    }

    @Test
    fun defaultSheepArtworkIsWhite() {
        val sheep = SheepItem(id = 0, x = 0f, y = 0f, vx = 0f, vy = 0f)
        assertEquals(SheepArtwork.WHITE, sheep.artwork)
    }

    private fun runSteps(
        start: List<SheepItem>,
        steps: Int,
        dt: Float
    ): List<SheepItem> {
        var current = start
        repeat(steps) { current = step(current, deltaSeconds = dt) }
        return current
    }
}
