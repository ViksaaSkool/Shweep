package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MockDailySheepQuotaRepository(
    initialDailyLimit: Int = SHEEP_QUOTA_LIMIT
) : DailySheepQuotaRepository {

    private val _quota = MutableStateFlow(
        DailySheepQuota(
            dailyLimit = initialDailyLimit,
            usedInWindow = 0,
            paywallShownAtEpochMillis = 0L,
            nextResetEpochMillis = 0L
        )
    )

    override val quota: Flow<DailySheepQuota> = _quota

    override suspend fun refresh(): DailySheepQuota {
        return _quota.value
    }

    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        val current = _quota.value
        if (current.isExhausted) {
            val locked = if (current.paywallShownAtEpochMillis <= 0L) {
                current.copy(
                    paywallShownAtEpochMillis = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                current
            }
            _quota.value = locked
            return ConsumeSheepResult.Exhausted(locked)
        }
        val consumed = current.copy(usedInWindow = current.usedInWindow + 1)
        _quota.value = consumed
        return ConsumeSheepResult.Allowed(consumed)
    }
}
