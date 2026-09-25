package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local, device-only persistence for the free sheep allowance.
 *
 * The persisted keys keep their original names (`daily_sheep_used`, `paywall_shown_at`) so that an
 * already-installed build keeps its current allowance instead of being granted a fresh one. The
 * code symbols describe what the values actually mean: a used-sheep count and the timestamp the
 * cooldown started.
 */
@OptIn(ExperimentalTime::class)
class DataStoreFreeSheepUsageRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.System
) : FreeSheepUsageRepository {

    private companion object {
        val USED_SHEEP_COUNT_KEY = intPreferencesKey("daily_sheep_used")
        val COOLDOWN_STARTED_AT_KEY = longPreferencesKey("paywall_shown_at")
    }

    override val usage: Flow<FreeSheepUsage> = dataStore.data.map { preferences ->
        FreeSheepUsagePolicy.toUsage(
            FreeSheepUsagePolicy.resolve(readStored(preferences), nowMillis())
        )
    }

    override suspend fun refresh(): FreeSheepUsage {
        var usage = FreeSheepUsagePolicy.toUsage(
            StoredFreeSheepUsage(usedSheepCount = 0, cooldownStartedAtEpochMillis = 0L)
        )

        dataStore.edit { preferences ->
            val resolved = FreeSheepUsagePolicy.startCooldownIfExhausted(
                readStored(preferences),
                nowMillis()
            )
            writeStored(preferences, resolved)
            usage = FreeSheepUsagePolicy.toUsage(resolved)
        }

        return usage
    }

    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        var result: ConsumeSheepResult = ConsumeSheepResult.Exhausted(
            FreeSheepUsagePolicy.toUsage(
                StoredFreeSheepUsage(
                    usedSheepCount = FREE_SHEEP_LIMIT,
                    cooldownStartedAtEpochMillis = 0L
                )
            )
        )

        dataStore.edit { preferences ->
            when (val step = FreeSheepUsagePolicy.consume(readStored(preferences), nowMillis())) {
                is ConsumeSheepStep.Allowed -> {
                    writeStored(preferences, step.usage)
                    result = ConsumeSheepResult.Allowed(FreeSheepUsagePolicy.toUsage(step.usage))
                }
                is ConsumeSheepStep.Exhausted -> {
                    writeStored(preferences, step.usage)
                    result = ConsumeSheepResult.Exhausted(FreeSheepUsagePolicy.toUsage(step.usage))
                }
            }
        }

        return result
    }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    private fun readStored(preferences: Preferences): StoredFreeSheepUsage = StoredFreeSheepUsage(
        usedSheepCount = preferences[USED_SHEEP_COUNT_KEY] ?: 0,
        cooldownStartedAtEpochMillis = preferences[COOLDOWN_STARTED_AT_KEY] ?: 0L
    )

    private fun writeStored(preferences: MutablePreferences, stored: StoredFreeSheepUsage) {
        preferences[USED_SHEEP_COUNT_KEY] = stored.usedSheepCount
        preferences[COOLDOWN_STARTED_AT_KEY] = stored.cooldownStartedAtEpochMillis
    }
}
