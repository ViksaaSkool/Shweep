package com.skooldev.shweep.screens

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
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
        deltaSeconds: Float
    ): List<SheepItem> = SheepSimulation.step(
        sheepList = sheep,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        playAreaStartY = playAreaStartY,
        sheepBaseSizePx = baseSizePx,
        deltaSeconds = deltaSeconds
    )

    @Test
    fun movementIsFrameRateIndependent() {
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 120f, vy = -60f, lifetimeSeconds = 999f, nextZigzagTurnIn = 999f, zigzagTurnInterval = 999f))

        val at60Hz = runSteps(start, steps = 60, dt = 1f / 60f)
        val at120Hz = runSteps(start, steps = 120, dt = 1f / 120f)
        val at30Hz = runSteps(start, steps = 30, dt = 1f / 30f)

        assertEquals(at60Hz[0].x, at120Hz[0].x, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].y, at120Hz[0].y, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].x, at30Hz[0].x, absoluteTolerance = 0.01f)
        assertEquals(at60Hz[0].y, at30Hz[0].y, absoluteTolerance = 0.01f)
    }

    @Test
    fun boundaryCollisionReflectsOnlyAffectedAxis() {
        val start = listOf(SheepItem(id = 0, x = screenWidth - baseSizePx - 1f, y = 900f, vx = 100f, vy = 50f, lifetimeSeconds = 999f))
        val result = step(start, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)

        assertEquals(screenWidth - baseSizePx, result[0].x, absoluteTolerance = 0.001f)
        assertTrue(result[0].vx < 0f)
        assertTrue(result[0].vy > 0f)
    }

    @Test
    fun topBoundaryClampsToPlayArea() {
        val start = listOf(SheepItem(id = 0, x = 500f, y = playAreaStartY - 50f, vx = 10f, vy = -100f, lifetimeSeconds = 999f))
        val result = step(start, deltaSeconds = 1f)

        assertEquals(playAreaStartY, result[0].y)
        assertTrue(result[0].vy > 0f)
    }

    @Test
    fun headOnCollisionExchangesNormalVelocity() {
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = 200f, vy = 0f, lifetimeSeconds = 999f)
        val b = SheepItem(id = 1, x = 440f + 1f, y = 900f, vx = -200f, vy = 0f, lifetimeSeconds = 999f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        assertTrue(result[0].vx < 0f, "a should now move away leftward")
        assertTrue(result[1].vx > 0f, "b should now move away rightward")
    }

    @Test
    fun glancingCollisionPreservesTangentialVelocity() {
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = 200f, vy = 0f, lifetimeSeconds = 999f)
        val b = SheepItem(id = 1, x = 420f, y = 980f, vx = -200f, vy = 0f, lifetimeSeconds = 999f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

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

        val changedA = result[0].vx != a.vx || result[0].vy != a.vy
        val changedB = result[1].vx != b.vx || result[1].vy != b.vy
        assertTrue(changedA)
        assertTrue(changedB)
    }

    @Test
    fun separatingOverlapIsCorrectedWithoutImpulse() {
        val a = SheepItem(id = 0, x = 300f, y = 900f, vx = -200f, vy = 0f, lifetimeSeconds = 999f)
        val b = SheepItem(id = 1, x = 380f, y = 900f, vx = 200f, vy = 0f, lifetimeSeconds = 999f)
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
        val a = SheepItem(id = 0, x = 500f, y = 900f, vx = 100f, vy = 100f, lifetimeSeconds = 999f)
        val b = SheepItem(id = 1, x = 500f, y = 900f, vx = -100f, vy = -100f, lifetimeSeconds = 999f)
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
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 250f, vy = 0f, lifetimeSeconds = 999f))
        val expected = SheepSimulation.MAX_DELTA_SECONDS * 250f
        val result = step(start, deltaSeconds = 2f)

        assertEquals(400f + expected, result[0].x, absoluteTolerance = 0.001f)
    }

    @Test
    fun collisionSeparationStaysInsidePlayArea() {
        val a = SheepItem(id = 0, x = 0f, y = 900f, vx = 200f, vy = 0f, lifetimeSeconds = 999f)
        val b = SheepItem(id = 1, x = 100f, y = 900f, vx = -200f, vy = 0f, lifetimeSeconds = 999f)
        val result = step(listOf(a, b), deltaSeconds = 1f / 120f)

        for (sheep in result) {
            assertTrue(sheep.x >= 0f, "sheep pushed through left wall")
            assertTrue(sheep.x <= screenWidth - baseSizePx, "sheep pushed through right wall")
            assertTrue(sheep.y >= playAreaStartY)
            assertTrue(sheep.y <= screenHeight - baseSizePx)
        }
    }

    @Test
    fun driftingSheepMoveOffScreenWithoutBounce() {
        val driftingLeft = SheepItem(
            id = 0, x = 100f, y = 1000f, vx = 0f, vy = 200f,
            isDriftingAway = true, driftSpeedX = -500f
        )
        val driftingRight = SheepItem(
            id = 1, x = 700f, y = 1000f, vx = 0f, vy = 200f,
            isDriftingAway = true, driftSpeedX = 500f
        )
        val result = step(listOf(driftingLeft, driftingRight), deltaSeconds = 1f)

        assertTrue(result[0].x < driftingLeft.x, "left drifter should keep moving left")
        assertTrue(result[1].x > driftingRight.x, "right drifter should keep moving right")
        assertEquals(driftingLeft.vy, result[0].vy)
        assertEquals(driftingRight.vy, result[1].vy)
    }

    @Test
    fun driftingSheepAreIgnoredByCollisionResolver() {
        val active = SheepItem(id = 0, x = 100f, y = 1000f, vx = 400f, vy = 0f, lifetimeSeconds = 999f)
        val drifting = SheepItem(
            id = 1, x = 180f, y = 1000f, vx = -400f, vy = 0f,
            isDriftingAway = true, driftSpeedX = -500f
        )
        val result = step(listOf(active, drifting), deltaSeconds = 1f / 60f)

        assertEquals(400f, result[0].vx, absoluteTolerance = 0.001f)
        assertTrue(result[0].x > active.x)
        assertEquals(-500f, result[1].driftSpeedX)
    }

    @Test
    fun sheepTransitionsToDriftingAfterLifetime() {
        val sheep = SheepItem(
            id = 0, x = 400f, y = 1000f, vx = 100f, vy = 50f,
            lifetimeSeconds = 0.1f, nextZigzagTurnIn = 999f, zigzagTurnInterval = 999f
        )
        var current = listOf(sheep)
        repeat(20) { current = step(current, deltaSeconds = 0.05f) }

        assertTrue(current[0].isDriftingAway, "sheep should be drifting after lifetime expires")
        assertTrue(current[0].driftSpeedX != 0f, "drift speed should be assigned")
    }

    @Test
    fun sheepRemainsActiveBeforeLifetime() {
        val sheep = SheepItem(
            id = 0, x = 400f, y = 1000f, vx = 100f, vy = 50f,
            lifetimeSeconds = 10f, nextZigzagTurnIn = 999f, zigzagTurnInterval = 999f
        )
        var current = listOf(sheep)
        repeat(2) { current = step(current, deltaSeconds = 0.05f) }

        assertFalse(current[0].isDriftingAway, "sheep should still be active")
        assertEquals(0.1f, current[0].ageSeconds, absoluteTolerance = 0.001f)
    }

    @Test
    fun zigZagTurnChangesVelocityDirection() {
        val sheep = SheepItem(
            id = 0, x = 400f, y = 1000f, vx = 200f, vy = 0f,
            lifetimeSeconds = 999f,
            nextZigzagTurnIn = 0.01f,
            zigzagTurnInterval = 1f
        )
        val result = step(listOf(sheep), deltaSeconds = 0.05f)

        val initialAngle = atan2(sheep.vy.toDouble(), sheep.vx.toDouble())
        val resultAngle = atan2(result[0].vy.toDouble(), result[0].vx.toDouble())
        val angleDiff = kotlin.math.abs(resultAngle - initialAngle)
        assertTrue(angleDiff > 0.01, "velocity direction should have changed after zig-zag turn")
    }

    @Test
    fun zigZagPreservesSpeed() {
        val sheep = SheepItem(
            id = 0, x = 400f, y = 1000f, vx = 200f, vy = 100f,
            lifetimeSeconds = 999f,
            nextZigzagTurnIn = 0.01f,
            zigzagTurnInterval = 1f
        )
        val initialSpeed = sqrt(sheep.vx * sheep.vx + sheep.vy * sheep.vy)
        val result = step(listOf(sheep), deltaSeconds = 0.05f)
        val resultSpeed = sqrt(result[0].vx * result[0].vx + result[0].vy * result[0].vy)

        assertEquals(initialSpeed, resultSpeed, absoluteTolerance = 0.1f, "speed should be preserved after zig-zag")
    }

    @Test
    fun zigZagDirectionAlternates() {
        val sheep = SheepItem(
            id = 0, x = 400f, y = 1000f, vx = 200f, vy = 0f,
            lifetimeSeconds = 999f,
            zigzagDirection = 1f,
            nextZigzagTurnIn = 0.01f,
            zigzagTurnInterval = 1f
        )
        val result = step(listOf(sheep), deltaSeconds = 0.05f)

        assertEquals(-1f, result[0].zigzagDirection, "direction should alternate")
    }

    @Test
    fun startDriftingChoosesLeftForLeftSide() {
        val sheep = SheepItem(id = 0, x = 100f, y = 1000f, vx = 200f, vy = 0f)
        val result = startDrifting(sheep, screenWidth, baseSizePx)

        assertTrue(result.isDriftingAway)
        assertTrue(result.driftSpeedX < 0f, "should drift left")
    }

    @Test
    fun startDriftingChoosesRightForRightSide() {
        val sheep = SheepItem(id = 0, x = 700f, y = 1000f, vx = -200f, vy = 0f)
        val result = startDrifting(sheep, screenWidth, baseSizePx)

        assertTrue(result.isDriftingAway)
        assertTrue(result.driftSpeedX > 0f, "should drift right")
    }

    @Test
    fun inactivityEventuallyClearsAllSheep() {
        var sheep = listOf(
            SheepItem(id = 0, x = 400f, y = 1000f, vx = 100f, vy = 50f, lifetimeSeconds = 1f),
            SheepItem(id = 1, x = 600f, y = 1200f, vx = -80f, vy = 30f, lifetimeSeconds = 1.5f)
        )

        repeat(500) {
            sheep = step(sheep, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)
            sheep = sheep.filter { shouldKeepSheep(it, screenWidth, screenHeight, playAreaStartY, baseSizePx) }
        }

        assertTrue(sheep.isEmpty(), "all sheep should have left after enough time")
    }

    @Test
    fun gaitPhaseAdvancesWithMovement() {
        val start = listOf(SheepItem(id = 0, x = 400f, y = 900f, vx = 250f, vy = 0f, gaitPhaseRadians = 0.5f, lifetimeSeconds = 999f))
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
            SheepItem(id = 0, x = screenWidth - baseSizePx - 1f, y = 900f, vx = 200f, vy = 0f, gaitPhaseRadians = 2.2f, lifetimeSeconds = 999f)
        )
        val result = step(nearWall, deltaSeconds = SheepSimulation.MAX_DELTA_SECONDS)

        assertTrue(result[0].vx < 0f, "sheep should bounce off the wall")
        assertTrue(result[0].gaitPhaseRadians > 2.2f, "phase must keep advancing through the bounce")
        assertTrue(result[0].gaitPhaseRadians < SheepGait.TAU)
    }

    @Test
    fun collisionSeparationDoesNotAdvanceGaitPhase() {
        val overlapping = listOf(
            SheepItem(id = 0, x = 300f, y = 900f, vx = -200f, vy = 0f, gaitPhaseRadians = 1.3f, lifetimeSeconds = 999f),
            SheepItem(id = 1, x = 380f, y = 900f, vx = 200f, vy = 0f, gaitPhaseRadians = 4.1f, lifetimeSeconds = 999f)
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
