package com.skooldev.shweep.screens

import kotlin.math.abs

enum class GestureMode { FLICK, DRAG }

data class GestureSample(
    val mode: GestureMode,
    val durationMillis: Long,
    val upwardDistanceDp: Float,
    val effectiveSpeedDpPerSecond: Float,
    val completed: Boolean,
    val cancelled: Boolean
)

private data class TimedGestureSample(
    val gesture: GestureSample,
    val elapsedMillis: Long
)

/**
 * Estimates how "asleep" the user is, expressed as grayness.
 *
 *  0f = awake / fully colorful
 *  1f = fully grayscale
 *
 * Inactivity is the primary, predictable signal. Declining swipe cadence and
 * gesture quality add smaller secondary contributions. The estimator starts at
 * 0 (colorful) and only rises as interaction slows.
 */
class SleepinessEstimator {

    private val samples = ArrayDeque<TimedGestureSample>()
    private val maxSamples = 24

    // Per-mode speed baselines (exponentially weighted moving average).
    private var flickBaselineSpeed = 0f
    private var dragBaselineSpeed = 0f

    // Cadence baseline built from the first successful intervals.
    private val intervals = ArrayDeque<Float>()
    private val warmupIntervals = ArrayList<Float>()
    private val warmupTarget = 5
    private var baselineIntervalMillis = 0f

    private var lastSuccessfulElapsedMillis = 0L
    private var lastPointerElapsedMillis = 0L
    private var sessionStartElapsedMillis = 0L
    private var smoothedGrayness = 0f

    val grayness: Float get() = smoothedGrayness

    fun reset(sessionStartElapsedMillis: Long) {
        samples.clear()
        flickBaselineSpeed = 0f
        dragBaselineSpeed = 0f
        intervals.clear()
        warmupIntervals.clear()
        baselineIntervalMillis = 0f
        lastSuccessfulElapsedMillis = sessionStartElapsedMillis
        lastPointerElapsedMillis = sessionStartElapsedMillis
        this.sessionStartElapsedMillis = sessionStartElapsedMillis
        smoothedGrayness = 0f
    }

    fun recordAttempt(sample: GestureSample, elapsedMillis: Long) {
        addSample(sample.copy(completed = false), elapsedMillis)
        lastPointerElapsedMillis = elapsedMillis
    }

    fun recordSuccess(sample: GestureSample, elapsedMillis: Long) {
        addSample(sample.copy(completed = true), elapsedMillis)
        lastPointerElapsedMillis = elapsedMillis

        if (lastSuccessfulElapsedMillis > sessionStartElapsedMillis) {
            val interval = (elapsedMillis - lastSuccessfulElapsedMillis).toFloat()
            recordInterval(interval)
        }
        lastSuccessfulElapsedMillis = elapsedMillis
        updateSpeedBaseline(sample)
    }

    fun onPointerActivity(elapsedMillis: Long) {
        lastPointerElapsedMillis = elapsedMillis
    }

    fun tick(elapsedMillis: Long, deltaSeconds: Float) {
        val idleSeconds = (elapsedMillis - lastPointerElapsedMillis) / 1000f
        val idleGrayness = smoothstep(30f, 180f, idleSeconds)

        val cadenceScore = computeCadenceScore()
        val qualityScore = computeQualityScore(elapsedMillis)

        val cadenceGrayness = cadenceScore * 0.45f
        val qualityGrayness = qualityScore * 0.20f

        val targetGrayness =
            1f -
                (1f - idleGrayness) *
                (1f - cadenceGrayness) *
                (1f - qualityGrayness)

        val delta = deltaSeconds.coerceAtMost(0.25f)
        val rate = if (targetGrayness > smoothedGrayness) {
            GRAYNESS_INCREASE_PER_SECOND
        } else {
            COLOR_RECOVERY_PER_SECOND
        }
        val step = rate * delta
        smoothedGrayness = moveTowards(smoothedGrayness, targetGrayness, step)
            .coerceIn(0f, 1f)
    }

    private fun addSample(sample: GestureSample, elapsedMillis: Long) {
        if (samples.size >= maxSamples) samples.removeFirst()
        samples.addLast(TimedGestureSample(sample, elapsedMillis))
    }

    private fun recordInterval(intervalMillis: Float) {
        intervals.addLast(intervalMillis)
        if (intervals.size > 12) intervals.removeFirst()

        if (baselineIntervalMillis <= 0f && warmupIntervals.size < warmupTarget) {
            warmupIntervals.add(intervalMillis)
            if (warmupIntervals.size >= warmupTarget) {
                baselineIntervalMillis = median(warmupIntervals)
            }
        }
    }

    private fun updateSpeedBaseline(sample: GestureSample) {
        if (sample.mode == GestureMode.FLICK) {
            flickBaselineSpeed = if (flickBaselineSpeed <= 0f) {
                sample.effectiveSpeedDpPerSecond
            } else {
                flickBaselineSpeed * 0.7f + sample.effectiveSpeedDpPerSecond * 0.3f
            }
        } else {
            dragBaselineSpeed = if (dragBaselineSpeed <= 0f) {
                sample.effectiveSpeedDpPerSecond
            } else {
                dragBaselineSpeed * 0.7f + sample.effectiveSpeedDpPerSecond * 0.3f
            }
        }
    }

    private fun computeCadenceScore(): Float {
        if (baselineIntervalMillis <= 0f || intervals.size < 2) return 0f
        val recent = intervals.toList().takeLast(6)
        val recentMedian = median(recent)
        val ratio = recentMedian / baselineIntervalMillis
        return smoothstep(1.5f, 3.0f, ratio)
    }

    private fun computeQualityScore(nowElapsedMillis: Long): Float {
        val recent = samples
            .filter { nowElapsedMillis - it.elapsedMillis <= RECENT_WINDOW_MILLIS }
            .takeLast(5)
        if (recent.isEmpty()) return 0f

        val failureRate = recent.count { !it.gesture.completed }.toFloat() / recent.size
        val failureScore = smoothstep(0.2f, 0.6f, failureRate)

        val completedRecent = recent.filter { it.gesture.completed }
        val speedScore = if (completedRecent.isNotEmpty()) {
            val ratios = completedRecent.map { s ->
                val baseline = if (s.gesture.mode == GestureMode.FLICK) {
                    flickBaselineSpeed
                } else {
                    dragBaselineSpeed
                }
                if (baseline > 0f && s.gesture.effectiveSpeedDpPerSecond > 0f) {
                    s.gesture.effectiveSpeedDpPerSecond / baseline
                } else {
                    1f
                }
            }
            val avgRatio = ratios.average().toFloat()
            1f - smoothstep(0.4f, 0.7f, avgRatio)
        } else 0f

        return (failureScore * 0.6f + speedScore * 0.4f).coerceIn(0f, 1f)
    }

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun moveTowards(current: Float, target: Float, maxDelta: Float): Float {
        val diff = target - current
        return when {
            abs(diff) <= maxDelta -> target
            diff > 0 -> current + maxDelta
            else -> current - maxDelta
        }
    }

    companion object {
        private const val GRAYNESS_INCREASE_PER_SECOND = 0.05f
        private const val COLOR_RECOVERY_PER_SECOND = 1f / 3f
        private const val RECENT_WINDOW_MILLIS = 30_000L
    }
}
