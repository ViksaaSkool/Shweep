package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessions: Flow<List<Session>>
    suspend fun addSession(session: Session)
    suspend fun clearAllSessions()
}