package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MockDailySheepQuotaRepository(
    initialDailyLimit: Int = 100
) : DailySheepQuotaRepository {

    private val _quota = MutableStateFlow(
        DailySheepQuota(
            dailyLimit = initialDailyLimit,
            usedToday = 0,
            periodStartEpochMillis = Clock.System.now().toEpochMilliseconds(),
            nextResetEpochMillis = Clock.System.now().toEpochMilliseconds() + 12 * 3_600_000
        )
    )

    override val quota: Flow<DailySheepQuota> = _quota

    override suspend fun refresh(): DailySheepQuota {
        return _quota.value
    }

    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        val current = _quota.value
        if (current.isExhausted) {
            return ConsumeSheepResult.Exhausted(current)
        }
        val consumed = current.copy(usedToday = current.usedToday + 1)
        _quota.value = consumed
        return ConsumeSheepResult.Allowed(consumed)
    }
}
