package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** In-memory allowance used by previews. Production always uses the DataStore implementation. */
@OptIn(ExperimentalTime::class)
class MockFreeSheepUsageRepository(
    initialFreeLimit: Int = FREE_SHEEP_LIMIT
) : FreeSheepUsageRepository {

    private val _usage = MutableStateFlow(
        FreeSheepUsage(
            freeLimit = initialFreeLimit,
            usedSheepCount = 0,
            cooldownStartedAtEpochMillis = 0L,
            cooldownEndsAtEpochMillis = 0L
        )
    )

    override val usage: Flow<FreeSheepUsage> = _usage

    override suspend fun refresh(): FreeSheepUsage {
        return _usage.value
    }

    override suspend fun tryConsumeSheep(): ConsumeSheepResult {
        val current = _usage.value
        if (current.isExhausted) {
            val locked = if (current.cooldownStartedAtEpochMillis <= 0L) {
                current.copy(
                    cooldownStartedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                current
            }
            _usage.value = locked
            return ConsumeSheepResult.Exhausted(locked)
        }
        val consumed = current.copy(usedSheepCount = current.usedSheepCount + 1)
        _usage.value = consumed
        return ConsumeSheepResult.Allowed(consumed)
    }
}
