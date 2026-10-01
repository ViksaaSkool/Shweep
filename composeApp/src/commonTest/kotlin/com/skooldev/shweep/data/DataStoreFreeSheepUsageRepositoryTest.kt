package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DataStoreFreeSheepUsageRepositoryTest {

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private val start = 1_700_000_000_000L

    private fun repo(
        dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore(),
        clock: Clock = MutableClock(start)
    ) = DataStoreFreeSheepUsageRepository(dataStore, clock)

    @Test
    fun freshInstallationHasAFullAllowance() = runTest {
        val usage = repo().refresh()

        assertEquals(FREE_SHEEP_LIMIT, usage.remaining)
        assertEquals(0, usage.usedSheepCount)
    }

    @Test
    fun consumptionPersistsAcrossRepositoryInstances() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val clock = MutableClock(start)

        val first = DataStoreFreeSheepUsageRepository(dataStore, clock)
        repeat(5) { first.tryConsumeSheep() }

        val reopened = DataStoreFreeSheepUsageRepository(dataStore, clock)
        val usage = reopened.refresh()

        assertEquals(5, usage.usedSheepCount)
        assertEquals(FREE_SHEEP_LIMIT - 5, usage.remaining)
    }

    @Test
    fun theLastAllowedSheepExhaustsTheAllowanceAndStartsTheCooldown() = runTest {
        val clock = MutableClock(start)
        val repository = repo(clock = clock)

        var last: ConsumeSheepResult? = null
        repeat(FREE_SHEEP_LIMIT) {
            last = repository.tryConsumeSheep()
        }

        val allowed = assertIs<ConsumeSheepResult.Allowed>(last)
        assertTrue(allowed.usage.isExhausted)
        assertEquals(start, allowed.usage.cooldownStartedAtEpochMillis)
        assertEquals(start + FREE_SHEEP_COOLDOWN_MILLIS, allowed.usage.cooldownEndsAtEpochMillis)
    }

    @Test
    fun furtherSheepAreBlockedDuringCooldown() = runTest {
        val clock = MutableClock(start)
        val repository = repo(clock = clock)
        repeat(FREE_SHEEP_LIMIT) { repository.tryConsumeSheep() }

        val blocked = assertIs<ConsumeSheepResult.Exhausted>(repository.tryConsumeSheep())

        assertEquals(FREE_SHEEP_LIMIT, blocked.usage.usedSheepCount)
        assertEquals(start, blocked.usage.cooldownStartedAtEpochMillis)
    }

    @Test
    fun allowanceResetsAfterTheCooldownElapses() = runTest {
        val clock = MutableClock(start)
        val repository = repo(clock = clock)
        repeat(FREE_SHEEP_LIMIT) { repository.tryConsumeSheep() }

        clock.millis = start + FREE_SHEEP_COOLDOWN_MILLIS + 1

        val usage = repository.refresh()
        assertEquals(0, usage.usedSheepCount)
        assertEquals(FREE_SHEEP_LIMIT, usage.remaining)
    }

    @Test
    fun concurrentConsumptionNeverExceedsTheLimit() = runTest {
        val repository = repo()

        val results = List(FREE_SHEEP_LIMIT + 10) { repository.tryConsumeSheep() }
        val allowed = results.count { it is ConsumeSheepResult.Allowed }
        val blocked = results.count { it is ConsumeSheepResult.Exhausted }

        assertEquals(FREE_SHEEP_LIMIT, allowed)
        assertEquals(10, blocked)
    }

    @Test
    fun usageFlowReflectsPersistedState() = runTest {
        val repository = repo()
        repository.tryConsumeSheep()
        repository.tryConsumeSheep()

        val usage = repository.usage.first()
        assertEquals(2, usage.usedSheepCount)
    }
}
