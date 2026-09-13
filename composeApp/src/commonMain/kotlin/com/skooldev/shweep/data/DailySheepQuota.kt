package com.skooldev.shweep.data

data class DailySheepQuota(
    val dailyLimit: Int,
    val usedToday: Int,
    val periodStartEpochMillis: Long,
    val nextResetEpochMillis: Long
) {
    val remaining: Int
        get() = (dailyLimit - usedToday).coerceAtLeast(0)

    val isExhausted: Boolean
        get() = remaining == 0
}

sealed interface ConsumeSheepResult {
    data class Allowed(val quota: DailySheepQuota) : ConsumeSheepResult
    data class Exhausted(val quota: DailySheepQuota) : ConsumeSheepResult
}
