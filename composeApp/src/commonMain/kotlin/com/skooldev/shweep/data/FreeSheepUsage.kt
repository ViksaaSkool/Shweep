package com.skooldev.shweep.data

/**
 * Local snapshot of the free sheep allowance.
 *
 * Only [usedSheepCount] and [cooldownStartedAtEpochMillis] are persisted; the limit, remaining
 * count, and cooldown end are derived. Nothing here leaves the device.
 */
data class FreeSheepUsage(
    val freeLimit: Int,
    val usedSheepCount: Int,
    val cooldownStartedAtEpochMillis: Long,
    val cooldownEndsAtEpochMillis: Long
) {
    val remaining: Int
        get() = (freeLimit - usedSheepCount).coerceAtLeast(0)

    val isExhausted: Boolean
        get() = remaining == 0
}

sealed interface ConsumeSheepResult {
    data class Allowed(val usage: FreeSheepUsage) : ConsumeSheepResult
    data class Exhausted(val usage: FreeSheepUsage) : ConsumeSheepResult
}
