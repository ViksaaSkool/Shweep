package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockSessionRepository : SessionRepository {
    override val sessions: Flow<List<Session>> = flowOf(
        listOf(
            Session(
                id = "1",
                startTime = 1704067200000,
                endTime = 1704070800000,
                sheepCount = 42
            ),
            Session(
                id = "2",
                startTime = 1704153600000,
                endTime = 1704157200000,
                sheepCount = 35
            ),
            Session(
                id = "3",
                startTime = 1704240000000,
                endTime = 1704243600000,
                sheepCount = 28
            )
        )
    )

    override suspend fun addSession(session: Session) {
    }

    override suspend fun clearAllSessions() {
    }
}

class EmptySessionRepository : SessionRepository {
    override val sessions: Flow<List<Session>> = flowOf(emptyList())

    override suspend fun addSession(session: Session) {
    }

    override suspend fun clearAllSessions() {
    }
}