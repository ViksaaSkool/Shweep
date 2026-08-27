package com.skooldev.shweep

import com.skooldev.shweep.data.ActiveSessionCheckpoint
import com.skooldev.shweep.data.SessionEndReason
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.screens.GestureSample
import com.skooldev.shweep.screens.SleepinessEstimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CountingSessionCoordinator(
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope
) {
    private val estimator = SleepinessEstimator()

    private val _grayness = MutableStateFlow(0f)
    val grayness: StateFlow<Float> = _grayness.asStateFlow()

    private var sessionId: String = ""
    private var sessionStartEpochMillis: Long = 0L
    private var lastInteractionEpochMillis: Long = 0L
    private var sheepCount: Int = 0
    private var backgroundedAtEpochMillis: Long? = null
    private var backgroundedAtElapsedMillis: Long? = null
    private var isActive: Boolean = false

    @OptIn(ExperimentalUuidApi::class)
    fun startSession() {
        sessionId = Uuid.random().toString()
        sessionStartEpochMillis = Clock.System.now().toEpochMilliseconds()
        lastInteractionEpochMillis = sessionStartEpochMillis
        sheepCount = 0
        backgroundedAtEpochMillis = null
        backgroundedAtElapsedMillis = null
        isActive = true
        _grayness.value = 0f

        estimator.reset(MonotonicClock.nowMillis())

        scope.launch {
            sessionRepository.saveActiveCheckpoint(
                ActiveSessionCheckpoint(
                    id = sessionId,
                    startTime = sessionStartEpochMillis,
                    lastInteractionTime = lastInteractionEpochMillis,
                    sheepCount = 0
                )
            )
        }
    }

    fun recordAttempt(sample: GestureSample, elapsedMillis: Long) {
        if (!isActive) return
        estimator.recordAttempt(sample, elapsedMillis)
    }

    fun recordSuccess(sample: GestureSample, elapsedMillis: Long) {
        if (!isActive) return
        estimator.recordSuccess(sample, elapsedMillis)
    }

    fun onPointerActivity(elapsedMillis: Long) {
        if (!isActive) return
        estimator.onPointerActivity(elapsedMillis)
    }

    fun incrementSheep() {
        if (!isActive) return
        sheepCount++
        lastInteractionEpochMillis = Clock.System.now().toEpochMilliseconds()
        persistCheckpoint()
    }

    fun tick(elapsedMillis: Long, deltaSeconds: Float) {
        if (!isActive) return
        estimator.tick(elapsedMillis, deltaSeconds)
        val newGrayness = estimator.grayness
        if (abs(newGrayness - _grayness.value) >= GRAYNESS_UPDATE_EPSILON) {
            _grayness.value = newGrayness
        }
    }

    fun onBackground() {
        if (!isActive) return
        backgroundedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
        backgroundedAtElapsedMillis = MonotonicClock.nowMillis()
        persistCheckpoint()
    }

    fun onForeground(): ForegroundResult {
        if (!isActive) return ForegroundResult.NoSession

        val bgElapsed = backgroundedAtElapsedMillis ?: return ForegroundResult.NoSession
        val elapsed = MonotonicClock.nowMillis() - bgElapsed

        return if (elapsed >= BACKGROUND_TIMEOUT_MILLIS) {
            scope.launch {
                sessionRepository.completeActiveSession(
                    endTime = backgroundedAtEpochMillis ?: lastInteractionEpochMillis,
                    endReason = SessionEndReason.BACKGROUND_TIMEOUT
                )
            }
            isActive = false
            _grayness.value = 0f
            ForegroundResult.SessionEnded
        } else {
            backgroundedAtEpochMillis = null
            backgroundedAtElapsedMillis = null
            persistCheckpoint()
            ForegroundResult.Resumed
        }
    }

    fun endSession(reason: SessionEndReason) {
        if (!isActive) return
        isActive = false
        _grayness.value = 0f
        scope.launch {
            sessionRepository.completeActiveSession(
                endTime = Clock.System.now().toEpochMilliseconds(),
                endReason = reason
            )
        }
    }

    suspend fun recoverOrphanedSession() {
        val checkpoint = sessionRepository.loadActiveCheckpoint() ?: return
        val endTime = checkpoint.backgroundedAt ?: checkpoint.lastInteractionTime
        sessionRepository.completeActiveSession(
            endTime = endTime,
            endReason = SessionEndReason.RECOVERED_AFTER_TERMINATION
        )
    }

    private fun persistCheckpoint() {
        scope.launch {
            sessionRepository.saveActiveCheckpoint(
                ActiveSessionCheckpoint(
                    id = sessionId,
                    startTime = sessionStartEpochMillis,
                    lastInteractionTime = lastInteractionEpochMillis,
                    sheepCount = sheepCount,
                    backgroundedAt = backgroundedAtEpochMillis
                )
            )
        }
    }

    enum class ForegroundResult {
        NoSession,
        Resumed,
        SessionEnded
    }

    companion object {
        const val BACKGROUND_TIMEOUT_MILLIS = 10_000L
        private const val GRAYNESS_UPDATE_EPSILON = 0.002f
    }
}
