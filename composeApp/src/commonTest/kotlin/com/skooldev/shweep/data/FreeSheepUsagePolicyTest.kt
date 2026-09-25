package com.skooldev.shweep.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FreeSheepUsagePolicyTest {

    private val hour = 60L * 60L * 1000L
    private val start = 1_700_000_000_000L
    private val withinCooldown = FREE_SHEEP_COOLDOWN_MILLIS / 2

    private fun fresh() =
        StoredFreeSheepUsage(usedSheepCount = 0, cooldownStartedAtEpochMillis = 0L)

    private fun consumeAll(): StoredFreeSheepUsage {
        var stored = fresh()
        repeat(FREE_SHEEP_LIMIT) { index ->
            val step = FreeSheepUsagePolicy.consume(stored, start + index)
            stored = assertIs<ConsumeSheepStep.Allowed>(step).usage
        }
        return stored
    }

    @Test
    fun productionLimitIsThirtyFiveSheep() {
        assertEquals(35, FREE_SHEEP_LIMIT)
    }

    @Test
    fun productionCooldownIsTwentyFourHours() {
        assertEquals(24L * 60L * 60L * 1000L, FREE_SHEEP_COOLDOWN_MILLIS)
        assertEquals(86_400_000L, FREE_SHEEP_COOLDOWN_MILLIS)
    }

    @Test
    fun freshAllowanceHasFullLimit() {
        val usage = FreeSheepUsagePolicy.toUsage(fresh())

        assertEquals(FREE_SHEEP_LIMIT, usage.freeLimit)
        assertEquals(FREE_SHEEP_LIMIT, usage.remaining)
        assertFalse(usage.isExhausted)
        assertEquals(0L, usage.cooldownEndsAtEpochMillis)
    }

    @Test
    fun consumingTheFullAllowanceIsAllowed() {
        var stored = fresh()

        repeat(FREE_SHEEP_LIMIT) { index ->
            val step = FreeSheepUsagePolicy.consume(stored, start + index)
            stored = assertIs<ConsumeSheepStep.Allowed>(step).usage
        }

        assertEquals(FREE_SHEEP_LIMIT, stored.usedSheepCount)
        assertTrue(FreeSheepUsagePolicy.toUsage(stored).isExhausted)
    }

    @Test
    fun theLastFreeSheepStartsTheCooldown() {
        val stored = consumeAll()

        assertEquals(start + FREE_SHEEP_LIMIT - 1, stored.cooldownStartedAtEpochMillis)
        assertEquals(
            start + FREE_SHEEP_LIMIT - 1 + FREE_SHEEP_COOLDOWN_MILLIS,
            FreeSheepUsagePolicy.toUsage(stored).cooldownEndsAtEpochMillis
        )
    }

    @Test
    fun consumingPastTheAllowanceStaysLocked() {
        val stored = consumeAll()
        val cooldownStartedAt = stored.cooldownStartedAtEpochMillis

        val step = FreeSheepUsagePolicy.consume(stored, start + withinCooldown)

        val exhausted = assertIs<ConsumeSheepStep.Exhausted>(step).usage
        assertEquals(cooldownStartedAt, exhausted.cooldownStartedAtEpochMillis)
    }

    @Test
    fun cooldownHoldsUntilItElapses() {
        val startedAt = start + hour
        val locked = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val step = FreeSheepUsagePolicy.consume(locked, startedAt + FREE_SHEEP_COOLDOWN_MILLIS - 1)

        val exhausted = assertIs<ConsumeSheepStep.Exhausted>(step).usage
        assertEquals(startedAt, exhausted.cooldownStartedAtEpochMillis)
    }

    @Test
    fun allowanceResetsAtExactlyTheCooldownBoundary() {
        val startedAt = start + hour
        val locked = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val step = FreeSheepUsagePolicy.consume(locked, startedAt + FREE_SHEEP_COOLDOWN_MILLIS)

        val allowed = assertIs<ConsumeSheepStep.Allowed>(step).usage
        assertEquals(1, allowed.usedSheepCount)
        assertEquals(0L, allowed.cooldownStartedAtEpochMillis)
    }

    @Test
    fun retryingWhileLockedNeverExtendsTheCooldown() {
        val startedAt = start
        val locked = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val first = assertIs<ConsumeSheepStep.Exhausted>(
            FreeSheepUsagePolicy.consume(locked, startedAt + FREE_SHEEP_COOLDOWN_MILLIS / 4)
        )
        val second = assertIs<ConsumeSheepStep.Exhausted>(
            FreeSheepUsagePolicy.consume(first.usage, startedAt + FREE_SHEEP_COOLDOWN_MILLIS / 2)
        )

        assertEquals(startedAt, second.usage.cooldownStartedAtEpochMillis)
    }

    @Test
    fun clockRolledBackwardsKeepsTheLock() {
        val startedAt = start
        val locked = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val step = FreeSheepUsagePolicy.consume(locked, startedAt - 5 * hour)

        assertTrue(step is ConsumeSheepStep.Exhausted)
    }

    @Test
    fun startCooldownRecordsTheTimeWhenAllowanceIsSpent() {
        val spent = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = 0L
        )

        val marked = FreeSheepUsagePolicy.startCooldownIfExhausted(spent, start)

        assertEquals(start, marked.cooldownStartedAtEpochMillis)
    }

    @Test
    fun startCooldownKeepsTheFirstTime() {
        val startedAt = start
        val spent = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val marked = FreeSheepUsagePolicy.startCooldownIfExhausted(spent, startedAt + withinCooldown)

        assertEquals(startedAt, marked.cooldownStartedAtEpochMillis)
    }

    @Test
    fun startCooldownDoesNothingWhenAllowanceRemains() {
        val partiallyUsed = StoredFreeSheepUsage(
            usedSheepCount = 1,
            cooldownStartedAtEpochMillis = 0L
        )

        val marked = FreeSheepUsagePolicy.startCooldownIfExhausted(partiallyUsed, start)

        assertEquals(0L, marked.cooldownStartedAtEpochMillis)
        assertEquals(1, marked.usedSheepCount)
    }

    @Test
    fun resolveRestoresAllowanceAfterTheCooldown() {
        val startedAt = start
        val locked = StoredFreeSheepUsage(
            usedSheepCount = FREE_SHEEP_LIMIT,
            cooldownStartedAtEpochMillis = startedAt
        )

        val resolved = FreeSheepUsagePolicy.resolve(locked, startedAt + FREE_SHEEP_COOLDOWN_MILLIS + 1)

        assertEquals(0, resolved.usedSheepCount)
        assertEquals(0L, resolved.cooldownStartedAtEpochMillis)
    }
}
