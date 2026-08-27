package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class MockSessionRepository : SessionRepository {
    override val sessions: Flow<List<Session>> = flowOf(
        listOf(
            Session(
                id = "1",
                startTime = 1704067200000L,
                endTime = 1704067200000L + 14 * 60_000L,
                sheepCount = 18
            ),
            Session(
                id = "2",
                startTime = 1704153600000L,
                endTime = 1704153600000L + 27 * 60_000L,
                sheepCount = 35
            ),
            Session(
                id = "3",
                startTime = 1704240000000L,
                endTime = 1704240000000L + 48 * 60_000L,
                sheepCount = 62
            ),
            Session(
                id = "4",
                startTime = 1704326400000L,
                endTime = 1704326400000L + 45_000L,
                sheepCount = 2
            ),
            Session(
                id = "5",
                startTime = 1704412800000L,
                endTime = 1704412800000L + 72 * 60_000L,
                sheepCount = 74
            )
        )
    )

    private var activeCheckpoint: ActiveSessionCheckpoint? = null

    override suspend fun addSession(session: Session) {}

    override suspend fun clearAllSessions() {}

    override suspend fun saveActiveCheckpoint(checkpoint: ActiveSessionCheckpoint) {
        activeCheckpoint = checkpoint
    }

    override suspend fun loadActiveCheckpoint(): ActiveSessionCheckpoint? = activeCheckpoint

    override suspend fun clearActiveCheckpoint() {
        activeCheckpoint = null
    }

    override suspend fun completeActiveSession(endTime: Long, endReason: SessionEndReason) {
        activeCheckpoint = null
    }
}

class EmptySessionRepository : SessionRepository {
    override val sessions: Flow<List<Session>> = flowOf(emptyList())

    private var activeCheckpoint: ActiveSessionCheckpoint? = null

    override suspend fun addSession(session: Session) {}

    override suspend fun clearAllSessions() {}

    override suspend fun saveActiveCheckpoint(checkpoint: ActiveSessionCheckpoint) {
        activeCheckpoint = checkpoint
    }

    override suspend fun loadActiveCheckpoint(): ActiveSessionCheckpoint? = activeCheckpoint

    override suspend fun clearActiveCheckpoint() {
        activeCheckpoint = null
    }

    override suspend fun completeActiveSession(endTime: Long, endReason: SessionEndReason) {
        activeCheckpoint = null
    }
}
