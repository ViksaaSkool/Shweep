package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DailySheepQuotaRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.System
) : DailySheepQuotaRepository {

    private companion object {
        val USED_IN_WINDOW_KEY = intPreferencesKey("daily_sheep_used")
        val PAYWALL_SHOWN_AT_KEY = longPreferencesKey("paywall_shown_at")
    }

    override val quota: Flow<DailySheepQuota> = flow {
        emit(refresh())
    }

    override suspend fun refresh(): DailySheepQuota {
        var quota = SheepQuotaWindow.toQuota(StoredSheepQuota(usedInWindow = 0, paywallShownAtEpochMillis = 0L))

        dataStore.edit { preferences ->
            val stored = readStored(preferences)
            val resolved = SheepQuotaWindow.markPaywallShownIfExhausted(stored, nowMillis())
            writeStored(preferences, resolved)
            quota = SheepQuotaWindow.toQuota(resolved)
        }

        return quota
    }

    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        var result: ConsumeSheepResult = ConsumeSheepResult.Exhausted(
            SheepQuotaWindow.toQuota(
                StoredSheepQuota(
                    usedInWindow = SHEEP_QUOTA_LIMIT,
                    paywallShownAtEpochMillis = 0L
                )
            )
        )

        dataStore.edit { preferences ->
            when (val step = SheepQuotaWindow.consume(readStored(preferences), nowMillis())) {
                is SheepQuotaStep.Allowed -> {
                    writeStored(preferences, step.stored)
                    result = ConsumeSheepResult.Allowed(SheepQuotaWindow.toQuota(step.stored))
                }
                is SheepQuotaStep.Exhausted -> {
                    writeStored(preferences, step.stored)
                    result = ConsumeSheepResult.Exhausted(SheepQuotaWindow.toQuota(step.stored))
                }
            }
        }

        return result
    }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    private fun readStored(preferences: Preferences): StoredSheepQuota = StoredSheepQuota(
        usedInWindow = preferences[USED_IN_WINDOW_KEY] ?: 0,
        paywallShownAtEpochMillis = preferences[PAYWALL_SHOWN_AT_KEY] ?: 0L
    )

    private fun writeStored(preferences: MutablePreferences, stored: StoredSheepQuota) {
        preferences[USED_IN_WINDOW_KEY] = stored.usedInWindow
        preferences[PAYWALL_SHOWN_AT_KEY] = stored.paywallShownAtEpochMillis
    }
}
