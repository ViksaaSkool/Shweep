package com.skooldev.shweep.data

import com.skooldev.shweep.FeatureFlags

internal val SHEEP_QUOTA_LIMIT: Int =
    if (FeatureFlags.LOCAL_TEST_MODE) 3 else 50
internal val SHEEP_QUOTA_WINDOW_MILLIS: Long =
    if (FeatureFlags.LOCAL_TEST_MODE) 3L * 60L * 1000L else 24L * 60L * 60L * 1000L

internal data class StoredSheepQuota(
    val usedInWindow: Int,
    val paywallShownAtEpochMillis: Long
)

internal sealed interface SheepQuotaStep {
    data class Allowed(val stored: StoredSheepQuota) : SheepQuotaStep
    data class Exhausted(val stored: StoredSheepQuota) : SheepQuotaStep
}

/**
 * Pure decision logic for the rolling sheep allowance.
 *
 * A fresh allowance grants [SHEEP_QUOTA_LIMIT] sheep. Once the allowance is spent the paywall is
 * shown and its timestamp is recorded. The allowance only resets 24 hours after that timestamp,
 * which means the clock keeps running across app restarts and across day boundaries.
 */
internal object SheepQuotaWindow {

    fun toQuota(stored: StoredSheepQuota): DailySheepQuota = DailySheepQuota(
        dailyLimit = SHEEP_QUOTA_LIMIT,
        usedInWindow = stored.usedInWindow.coerceIn(0, SHEEP_QUOTA_LIMIT),
        paywallShownAtEpochMillis = stored.paywallShownAtEpochMillis,
        nextResetEpochMillis = stored.paywallShownAtEpochMillis
            .takeIf { it > 0L }
            ?.let { it + SHEEP_QUOTA_WINDOW_MILLIS }
            ?: 0L
    )

    /**
     * Applies the rolling window. When 24 hours have passed since the paywall was shown the
     * allowance is restored. A device clock that moved backwards keeps the lock in place.
     */
    fun resolve(stored: StoredSheepQuota, nowEpochMillis: Long): StoredSheepQuota {
        val shownAt = stored.paywallShownAtEpochMillis
        if (shownAt <= 0L) return stored

        return if (nowEpochMillis >= shownAt + SHEEP_QUOTA_WINDOW_MILLIS) {
            StoredSheepQuota(usedInWindow = 0, paywallShownAtEpochMillis = 0L)
        } else {
            stored
        }
    }

    fun consume(stored: StoredSheepQuota, nowEpochMillis: Long): SheepQuotaStep {
        val resolved = resolve(stored, nowEpochMillis)
        return if (resolved.usedInWindow < SHEEP_QUOTA_LIMIT) {
            SheepQuotaStep.Allowed(recordLockStartOnLastSheep(resolved, nowEpochMillis))
        } else {
            SheepQuotaStep.Exhausted(markPaywallShownIfExhausted(resolved, nowEpochMillis))
        }
    }

    /**
     * The 24 hour window starts when the last free sheep is spent, so the countdown is already
     * running if the user closes the app right after using the allowance.
     */
    private fun recordLockStartOnLastSheep(
        resolved: StoredSheepQuota,
        nowEpochMillis: Long
    ): StoredSheepQuota {
        val consumed = resolved.copy(usedInWindow = resolved.usedInWindow + 1)
        if (consumed.usedInWindow >= SHEEP_QUOTA_LIMIT &&
            consumed.paywallShownAtEpochMillis <= 0L
        ) {
            return consumed.copy(paywallShownAtEpochMillis = nowEpochMillis)
        }
        return consumed
    }

    /**
     * Records the moment the paywall is displayed when the allowance is already spent. An existing
     * timestamp is never overwritten, so dismissing and retrying cannot extend the lock.
     */
    fun markPaywallShownIfExhausted(
        stored: StoredSheepQuota,
        nowEpochMillis: Long
    ): StoredSheepQuota {
        val resolved = resolve(stored, nowEpochMillis)
        if (resolved.usedInWindow >= SHEEP_QUOTA_LIMIT &&
            resolved.paywallShownAtEpochMillis <= 0L
        ) {
            return resolved.copy(paywallShownAtEpochMillis = nowEpochMillis)
        }
        return resolved
    }
}
