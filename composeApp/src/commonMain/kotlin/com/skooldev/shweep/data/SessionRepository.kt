package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionRepository(private val dataStore: DataStore<Preferences>) {
    
    private val SESSIONS_KEY = stringPreferencesKey("sessions")
    
    val sessions: Flow<List<Session>> = dataStore.data
        .map { preferences ->
            val sessionsJson = preferences[SESSIONS_KEY] ?: "[]"
            Json.decodeFromString(sessionsJson)
        }
    
    suspend fun addSession(session: Session) {
        dataStore.edit { preferences ->
            val currentSessionsJson = preferences[SESSIONS_KEY] ?: "[]"
            val currentSessions = Json.decodeFromString<List<Session>>(currentSessionsJson)
            val updatedSessions = currentSessions + session
            preferences[SESSIONS_KEY] = Json.encodeToString(updatedSessions)
        }
    }
    
    suspend fun clearAllSessions() {
        dataStore.edit { preferences ->
            preferences[SESSIONS_KEY] = "[]"
        }
    }
}