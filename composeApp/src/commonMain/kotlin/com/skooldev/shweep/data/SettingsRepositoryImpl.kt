package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val SHEEP_COLOR_KEY = stringPreferencesKey("sheep_color")
    private val HAS_CHOSEN_SHEEP_COLOR_KEY = booleanPreferencesKey("has_chosen_sheep_color")
    private val SEEN_UPDATE_NOTICE_VERSION_KEY = stringPreferencesKey("seen_update_notice_version")

    override val sheepColor: Flow<SheepColor> = dataStore.data
        .map { preferences -> SheepColor.fromStorage(preferences[SHEEP_COLOR_KEY]) }

    override val hasChosenSheepColor: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[HAS_CHOSEN_SHEEP_COLOR_KEY] ?: false }

    override val seenUpdateNoticeVersion: Flow<String?> = dataStore.data
        .map { preferences -> preferences[SEEN_UPDATE_NOTICE_VERSION_KEY] }

    override suspend fun setSheepColor(color: SheepColor) {
        dataStore.edit { preferences ->
            preferences[SHEEP_COLOR_KEY] = color.storageValue
        }
    }

    override suspend fun markSheepColorChosen() {
        dataStore.edit { preferences ->
            preferences[HAS_CHOSEN_SHEEP_COLOR_KEY] = true
        }
    }

    override suspend fun markUpdateNoticeSeen(version: String) {
        dataStore.edit { preferences ->
            preferences[SEEN_UPDATE_NOTICE_VERSION_KEY] = version
        }
    }
}
