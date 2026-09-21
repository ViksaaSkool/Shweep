package com.skooldev.shweep.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SheepQuotaWindowTest {

    private val hour = 60L * 60L * 1000L
    private val start = 1_700_000_000_000L
    private val withinWindow = SHEEP_QUOTA_WINDOW_MILLIS / 2

    private fun fresh() = StoredSheepQuota(usedInWindow = 0, paywallShownAtEpochMillis = 0L)

    private fun consumeAll(): StoredSheepQuota {
        var stored = fresh()
        repeat(SHEEP_QUOTA_LIMIT) { index ->
            val step = SheepQuotaWindow.consume(stored, start + index)
            stored = assertIs<SheepQuotaStep.Allowed>(step).stored
        }
        return stored
    }

    @Test
    fun freshAllowanceHasFullLimit() {
        val quota = SheepQuotaWindow.toQuota(fresh())

        assertEquals(SHEEP_QUOTA_LIMIT, quota.dailyLimit)
        assertEquals(SHEEP_QUOTA_LIMIT, quota.remaining)
        assertFalse(quota.isExhausted)
        assertEquals(0L, quota.nextResetEpochMillis)
    }

    @Test
    fun consumingTheFullAllowanceIsAllowed() {
        var stored = fresh()

        repeat(SHEEP_QUOTA_LIMIT) { index ->
            val step = SheepQuotaWindow.consume(stored, start + index)
            stored = assertIs<SheepQuotaStep.Allowed>(step).stored
        }

        assertEquals(SHEEP_QUOTA_LIMIT, stored.usedInWindow)
        assertTrue(SheepQuotaWindow.toQuota(stored).isExhausted)
    }

    @Test
    fun theLastFreeSheepStartsTheLockWindow() {
        val stored = consumeAll()

        assertEquals(start + SHEEP_QUOTA_LIMIT - 1, stored.paywallShownAtEpochMillis)
        assertEquals(
            start + SHEEP_QUOTA_LIMIT - 1 + SHEEP_QUOTA_WINDOW_MILLIS,
            SheepQuotaWindow.toQuota(stored).nextResetEpochMillis
        )
    }

    @Test
    fun consumingPastTheAllowanceStaysLocked() {
        val stored = consumeAll()
        val lockStartedAt = stored.paywallShownAtEpochMillis

        val step = SheepQuotaWindow.consume(stored, start + withinWindow)

        val exhausted = assertIs<SheepQuotaStep.Exhausted>(step).stored
        assertEquals(lockStartedAt, exhausted.paywallShownAtEpochMillis)
    }

    @Test
    fun lockHoldsUntilTheWindowElapses() {
        val shownAt = start + hour
        val locked = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val step = SheepQuotaWindow.consume(locked, shownAt + SHEEP_QUOTA_WINDOW_MILLIS - 1)

        val exhausted = assertIs<SheepQuotaStep.Exhausted>(step).stored
        assertEquals(shownAt, exhausted.paywallShownAtEpochMillis)
    }

    @Test
    fun allowanceResetsAtExactlyTheWindowBoundary() {
        val shownAt = start + hour
        val locked = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val step = SheepQuotaWindow.consume(locked, shownAt + SHEEP_QUOTA_WINDOW_MILLIS)

        val allowed = assertIs<SheepQuotaStep.Allowed>(step).stored
        assertEquals(1, allowed.usedInWindow)
        assertEquals(0L, allowed.paywallShownAtEpochMillis)
    }

    @Test
    fun retryingWhileLockedNeverExtendsTheWindow() {
        val shownAt = start
        val locked = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val first = assertIs<SheepQuotaStep.Exhausted>(
            SheepQuotaWindow.consume(locked, shownAt + SHEEP_QUOTA_WINDOW_MILLIS / 4)
        )
        val second = assertIs<SheepQuotaStep.Exhausted>(
            SheepQuotaWindow.consume(first.stored, shownAt + SHEEP_QUOTA_WINDOW_MILLIS / 2)
        )

        assertEquals(shownAt, second.stored.paywallShownAtEpochMillis)
    }

    @Test
    fun clockRolledBackwardsKeepsTheLock() {
        val shownAt = start
        val locked = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val step = SheepQuotaWindow.consume(locked, shownAt - 5 * hour)

        assertTrue(step is SheepQuotaStep.Exhausted)
    }

    @Test
    fun markPaywallShownRecordsDateWhenAllowanceIsSpent() {
        val spent = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = 0L)

        val marked = SheepQuotaWindow.markPaywallShownIfExhausted(spent, start)

        assertEquals(start, marked.paywallShownAtEpochMillis)
    }

    @Test
    fun markPaywallShownKeepsTheFirstDate() {
        val shownAt = start
        val spent = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val marked = SheepQuotaWindow.markPaywallShownIfExhausted(spent, shownAt + withinWindow)

        assertEquals(shownAt, marked.paywallShownAtEpochMillis)
    }

    @Test
    fun markPaywallShownDoesNothingWhenAllowanceRemains() {
        val partiallyUsed = StoredSheepQuota(usedInWindow = 1, paywallShownAtEpochMillis = 0L)

        val marked = SheepQuotaWindow.markPaywallShownIfExhausted(partiallyUsed, start)

        assertEquals(0L, marked.paywallShownAtEpochMillis)
        assertEquals(1, marked.usedInWindow)
    }

    @Test
    fun resolveRestoresAllowanceAfterTheWindow() {
        val shownAt = start
        val locked = StoredSheepQuota(usedInWindow = SHEEP_QUOTA_LIMIT, paywallShownAtEpochMillis = shownAt)

        val resolved = SheepQuotaWindow.resolve(locked, shownAt + SHEEP_QUOTA_WINDOW_MILLIS + 1)

        assertEquals(0, resolved.usedInWindow)
        assertEquals(0L, resolved.paywallShownAtEpochMillis)
    }
}
