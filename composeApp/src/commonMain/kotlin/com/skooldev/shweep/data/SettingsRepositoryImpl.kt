package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val SHEEP_COLOR_KEY = stringPreferencesKey("sheep_color")

    override val sheepColor: Flow<SheepColor> = dataStore.data
        .map { preferences -> SheepColor.fromStorage(preferences[SHEEP_COLOR_KEY]) }

    override suspend fun setSheepColor(color: SheepColor) {
        dataStore.edit { preferences ->
            preferences[SHEEP_COLOR_KEY] = color.storageValue
        }
    }
}
