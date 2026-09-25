package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Stable, app-scoped device identifier.
 *
 * This is a random UUID generated on first use and stored locally. It is not an advertising
 * identifier and requires no permissions. It exists so that a future remote cooldown service
 * (Cloudflare Worker + Turso) can key the free-sheep cooldown by `device_id` without duplicating
 * RevenueCat purchase ownership. It is not transmitted anywhere yet.
 */
interface DeviceIdProvider {
    suspend fun deviceId(): String
}

@OptIn(ExperimentalUuidApi::class)
class DataStoreDeviceIdProvider(
    private val dataStore: DataStore<Preferences>
) : DeviceIdProvider {

    override suspend fun deviceId(): String {
        var id = ""
        dataStore.edit { preferences ->
            val existing = preferences[DEVICE_ID_KEY]
            if (existing.isNullOrBlank()) {
                val generated = Uuid.random().toString()
                preferences[DEVICE_ID_KEY] = generated
                id = generated
            } else {
                id = existing
            }
        }
        return id
    }

    private companion object {
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }
}
