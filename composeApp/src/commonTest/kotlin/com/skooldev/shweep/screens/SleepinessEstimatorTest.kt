package com.skooldev.shweep.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepinessEstimatorTest {

    private fun estimator(startedAt: Long = 0L): SleepinessEstimator {
        val e = SleepinessEstimator()
        e.reset(startedAt)
        return e
    }

    private fun success(
        mode: GestureMode = GestureMode.FLICK,
        speed: Float = 1000f
    ) = GestureSample(
        mode = mode,
        durationMillis = 300L,
        upwardDistanceDp = 100f,
        effectiveSpeedDpPerSecond = speed,
        completed = true,
        cancelled = false
    )

    @Test
    fun resetStartsFullyColorful() {
        assertEquals(0f, estimator().grayness, 0.0001f)
    }

    @Test
    fun shortInactivityKeepsFullColor() {
        val e = estimator()
        e.tick(elapsedMillis = 10_000L, deltaSeconds = 1f)
        assertEquals(0f, e.grayness, 0.0001f)
    }

    @Test
    fun inactivityIncreasesGraynessOverTime() {
        val e = estimator()
        val early = run {
            e.tick(elapsedMillis = 90_000L, deltaSeconds = 1f)
            e.grayness
        }
        val late = run {
            e.tick(elapsedMillis = 150_000L, deltaSeconds = 1f)
            e.grayness
        }
        assertTrue(late > early, "grayness should increase with inactivity")
        assertTrue(late <= 1f, "grayness must never exceed 1")
    }

    @Test
    fun graynessRecoversAfterSuccess() {
        val e = estimator()
        // Push grayness up through prolonged inactivity.
        for (i in 1..200) {
            e.tick(elapsedMillis = i * 1_000L, deltaSeconds = 1f)
        }
        assertTrue(e.grayness > 0.5f, "expected elevated grayness, got ${e.grayness}")

        // A successful swipe triggers fast recovery.
        val t0 = 210_000L
        e.recordSuccess(success(), t0)
        for (i in 1..60) {
            e.tick(elapsedMillis = t0 + i * 50L, deltaSeconds = 0.05f)
        }
        assertTrue(e.grayness < 0.2f, "grayness should recover quickly, got ${e.grayness}")
    }

    @Test
    fun firstSuccessDoesNotDefineCadence() {
        val e = estimator()
        e.recordSuccess(success(), 30_000L)
        e.recordSuccess(success(), 31_000L)
        // No warmup intervals yet, so cadence contributes nothing.
        e.tick(elapsedMillis = 31_000L, deltaSeconds = 1f)
        assertTrue(e.grayness < 0.1f, "first interval should not trigger cadence grayness")
    }

    @Test
    fun failedAttemptsContributeQuality() {
        val e = estimator()
        val t = 60_000L
        for (i in 0 until 5) {
            e.recordAttempt(
                GestureSample(
                    mode = GestureMode.FLICK,
                    durationMillis = 300L,
                    upwardDistanceDp = 10f,
                    effectiveSpeedDpPerSecond = 100f,
                    completed = false,
                    cancelled = false
                ),
                t + i * 1_000L
            )
        }
        e.tick(elapsedMillis = t + 5_000L, deltaSeconds = 1f)
        assertTrue(e.grayness > 0f, "failed attempts should raise grayness")
    }
}
