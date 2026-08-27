package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessions: Flow<List<Session>>
    suspend fun addSession(session: Session)
    suspend fun clearAllSessions()
    suspend fun saveActiveCheckpoint(checkpoint: ActiveSessionCheckpoint)
    suspend fun loadActiveCheckpoint(): ActiveSessionCheckpoint?
    suspend fun clearActiveCheckpoint()
    suspend fun completeActiveSession(endTime: Long, endReason: SessionEndReason)
}
