package com.skooldev.shweep.data

/** Number of free sheep granted per allowance. */
internal const val FREE_SHEEP_LIMIT: Int = 35

/** Length of the cooldown that starts when the final free sheep is counted. */
internal const val FREE_SHEEP_COOLDOWN_MILLIS: Long = 24L * 60L * 60L * 1000L

/**
 * The minimum state persisted locally for the free allowance.
 *
 * A [cooldownStartedAtEpochMillis] of `0L` means no cooldown is active. The cooldown end is
 * derived rather than stored, so only the minimum state is persisted.
 */
internal data class StoredFreeSheepUsage(
    val usedSheepCount: Int,
    val cooldownStartedAtEpochMillis: Long
)

internal sealed interface ConsumeSheepStep {
    data class Allowed(val usage: StoredFreeSheepUsage) : ConsumeSheepStep
    data class Exhausted(val usage: StoredFreeSheepUsage) : ConsumeSheepStep
}

/**
 * Pure decision logic for the local free allowance.
 *
 * A fresh allowance grants [FREE_SHEEP_LIMIT] sheep. Once the allowance is spent the cooldown
 * starts and its timestamp is recorded. The allowance only resets [FREE_SHEEP_COOLDOWN_MILLIS]
 * after that timestamp, so the clock keeps running across app restarts and across day boundaries.
 *
 * This is entirely local. Nothing here is sent to a backend, and no device identifier is involved.
 */
internal object FreeSheepUsagePolicy {

    fun toUsage(stored: StoredFreeSheepUsage): FreeSheepUsage = FreeSheepUsage(
        freeLimit = FREE_SHEEP_LIMIT,
        usedSheepCount = stored.usedSheepCount.coerceIn(0, FREE_SHEEP_LIMIT),
        cooldownStartedAtEpochMillis = stored.cooldownStartedAtEpochMillis,
        cooldownEndsAtEpochMillis = stored.cooldownStartedAtEpochMillis
            .takeIf { it > 0L }
            ?.let { it + FREE_SHEEP_COOLDOWN_MILLIS }
            ?: 0L
    )

    /**
     * Applies the cooldown. When the cooldown has elapsed the allowance is restored. A device
     * clock that moved backwards keeps the lock in place.
     */
    fun resolve(stored: StoredFreeSheepUsage, nowEpochMillis: Long): StoredFreeSheepUsage {
        val startedAt = stored.cooldownStartedAtEpochMillis
        if (startedAt <= 0L) return stored

        return if (nowEpochMillis >= startedAt + FREE_SHEEP_COOLDOWN_MILLIS) {
            StoredFreeSheepUsage(usedSheepCount = 0, cooldownStartedAtEpochMillis = 0L)
        } else {
            stored
        }
    }

    fun consume(stored: StoredFreeSheepUsage, nowEpochMillis: Long): ConsumeSheepStep {
        val resolved = resolve(stored, nowEpochMillis)
        return if (resolved.usedSheepCount < FREE_SHEEP_LIMIT) {
            ConsumeSheepStep.Allowed(startCooldownOnLastSheep(resolved, nowEpochMillis))
        } else {
            ConsumeSheepStep.Exhausted(startCooldownIfExhausted(resolved, nowEpochMillis))
        }
    }

    /**
     * The cooldown starts when the last free sheep is spent, so the countdown is already running
     * if the user closes the app right after using the allowance.
     */
    private fun startCooldownOnLastSheep(
        resolved: StoredFreeSheepUsage,
        nowEpochMillis: Long
    ): StoredFreeSheepUsage {
        val consumed = resolved.copy(usedSheepCount = resolved.usedSheepCount + 1)
        if (consumed.usedSheepCount >= FREE_SHEEP_LIMIT &&
            consumed.cooldownStartedAtEpochMillis <= 0L
        ) {
            return consumed.copy(cooldownStartedAtEpochMillis = nowEpochMillis)
        }
        return consumed
    }

    /**
     * Records the moment the cooldown starts when the allowance is already spent. An existing
     * timestamp is never overwritten, so dismissing and retrying cannot extend the cooldown.
     */
    fun startCooldownIfExhausted(
        stored: StoredFreeSheepUsage,
        nowEpochMillis: Long
    ): StoredFreeSheepUsage {
        val resolved = resolve(stored, nowEpochMillis)
        if (resolved.usedSheepCount >= FREE_SHEEP_LIMIT &&
            resolved.cooldownStartedAtEpochMillis <= 0L
        ) {
            return resolved.copy(cooldownStartedAtEpochMillis = nowEpochMillis)
        }
        return resolved
    }
}
