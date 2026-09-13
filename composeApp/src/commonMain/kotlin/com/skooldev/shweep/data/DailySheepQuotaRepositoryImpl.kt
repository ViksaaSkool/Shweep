package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DailySheepQuotaRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : DailySheepQuotaRepository {

    private companion object {
        const val DAILY_LIMIT = 50
        val USED_TODAY_KEY = intPreferencesKey("daily_sheep_used")
        val PERIOD_START_KEY = longPreferencesKey("daily_sheep_period_start")
        val NOON_TIME = LocalTime(12, 0)
    }

    private val timeZone: TimeZone
        get() = TimeZone.currentSystemDefault()

    override val quota: Flow<DailySheepQuota> = flow {
        emit(refresh())
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun refresh(): DailySheepQuota {
        return dataStore.data.first().let { preferences ->
            val stored = readStored(preferences)
            val now = Clock.System.now()
            applyRolloverIfNeeded(stored, now)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        var result: ConsumeSheepResult = ConsumeSheepResult.Exhausted(
            DailySheepQuota(0, 0, 0, 0)
        )

        dataStore.edit { preferences ->
            val stored = readStored(preferences)
            val now = Clock.System.now()
            val rolled = applyRolloverIfNeeded(stored, now)

            if (rolled.isExhausted) {
                result = ConsumeSheepResult.Exhausted(rolled)
            } else {
                val consumed = rolled.copy(
                    usedToday = rolled.usedToday + 1
                )
                preferences[USED_TODAY_KEY] = consumed.usedToday
                preferences[PERIOD_START_KEY] = consumed.periodStartEpochMillis
                result = ConsumeSheepResult.Allowed(consumed)
            }
        }

        return result
    }

    private fun applyRolloverIfNeeded(
        stored: DailySheepQuota,
        now: Instant
    ): DailySheepQuota {
        val currentPeriodStart = calculatePeriodStart(now)
        val nextReset = calculateNextReset(now)

        if (stored.periodStartEpochMillis == 0L) {
            return DailySheepQuota(
                dailyLimit = DAILY_LIMIT,
                usedToday = 0,
                periodStartEpochMillis = currentPeriodStart,
                nextResetEpochMillis = nextReset
            )
        }

        if (currentPeriodStart <= stored.periodStartEpochMillis) {
            return stored.copy(nextResetEpochMillis = nextReset)
        }

        return DailySheepQuota(
            dailyLimit = DAILY_LIMIT,
            usedToday = 0,
            periodStartEpochMillis = currentPeriodStart,
            nextResetEpochMillis = nextReset
        )
    }

    private fun calculatePeriodStart(now: Instant): Long {
        val localDateTime = now.toLocalDateTime(timeZone)
        val localDate = localDateTime.date
        val localTime = localDateTime.time

        val periodDate = if (localTime >= NOON_TIME) {
            localDate
        } else {
            localDate.minus(1, DateTimeUnit.DAY)
        }

        return LocalDateTime(periodDate, NOON_TIME)
            .toInstant(timeZone)
            .toEpochMilliseconds()
    }

    private fun calculateNextReset(now: Instant): Long {
        val localDateTime = now.toLocalDateTime(timeZone)
        val localDate = localDateTime.date
        val localTime = localDateTime.time

        val nextDate = if (localTime >= NOON_TIME) {
            localDate.plus(1, DateTimeUnit.DAY)
        } else {
            localDate
        }

        return LocalDateTime(nextDate, NOON_TIME)
            .toInstant(timeZone)
            .toEpochMilliseconds()
    }

    private fun readStored(preferences: Preferences): DailySheepQuota {
        val usedToday = preferences[USED_TODAY_KEY] ?: 0
        val periodStart = preferences[PERIOD_START_KEY] ?: 0L
        val nextReset = calculateNextReset(Clock.System.now())

        return DailySheepQuota(
            dailyLimit = DAILY_LIMIT,
            usedToday = usedToday,
            periodStartEpochMillis = periodStart,
            nextResetEpochMillis = nextReset
        )
    }
}
