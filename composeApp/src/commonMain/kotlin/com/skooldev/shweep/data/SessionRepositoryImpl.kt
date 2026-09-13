package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionRepositoryImpl(private val dataStore: DataStore<Preferences>) : SessionRepository {

    private val SESSIONS_KEY = stringPreferencesKey("sessions")
    private val ACTIVE_CHECKPOINT_KEY = stringPreferencesKey("active_session")

    private val json = Json { ignoreUnknownKeys = true }

    override val sessions: Flow<List<Session>> = dataStore.data
        .map { preferences ->
            val sessionsJson = preferences[SESSIONS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<Session>>(sessionsJson)
            } catch (_: Exception) {
                emptyList()
            }
        }

    override suspend fun addSession(session: Session) {
        dataStore.edit { preferences ->
            val currentSessionsJson = preferences[SESSIONS_KEY] ?: "[]"
            val currentSessions = try {
                json.decodeFromString<List<Session>>(currentSessionsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedSessions = currentSessions + session
            preferences[SESSIONS_KEY] = json.encodeToString(updatedSessions)
        }
    }

    override suspend fun clearAllSessions() {
        dataStore.edit { preferences ->
            preferences[SESSIONS_KEY] = "[]"
        }
    }

    override suspend fun saveActiveCheckpoint(checkpoint: ActiveSessionCheckpoint) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_CHECKPOINT_KEY] = json.encodeToString(checkpoint)
        }
    }

    override suspend fun loadActiveCheckpoint(): ActiveSessionCheckpoint? {
        val prefs = dataStore.data.first()
        val json = prefs[ACTIVE_CHECKPOINT_KEY] ?: return null
        return try {
            this.json.decodeFromString<ActiveSessionCheckpoint>(json)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearActiveCheckpoint() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_CHECKPOINT_KEY)
        }
    }

    override suspend fun completeActiveSession(endTime: Long, endReason: SessionEndReason) {
        val checkpoint = loadActiveCheckpoint() ?: return
        val session = Session(
            id = checkpoint.id,
            startTime = checkpoint.startTime,
            endTime = endTime,
            sheepCount = checkpoint.sheepCount,
            endReason = endReason
        )
        dataStore.edit { preferences ->
            val currentSessionsJson = preferences[SESSIONS_KEY] ?: "[]"
            val currentSessions = try {
                json.decodeFromString<List<Session>>(currentSessionsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val exists = currentSessions.any { it.id == session.id }
            val updatedSessions = if (exists) {
                currentSessions.map { if (it.id == session.id) session else it }
            } else {
                currentSessions + session
            }
            preferences[SESSIONS_KEY] = json.encodeToString(updatedSessions)
            preferences.remove(ACTIVE_CHECKPOINT_KEY)
        }
    }
}
