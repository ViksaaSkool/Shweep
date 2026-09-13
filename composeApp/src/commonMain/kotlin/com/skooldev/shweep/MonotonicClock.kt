package com.skooldev.shweep

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
internal object MonotonicClock {
    private val origin = TimeSource.Monotonic.markNow()

    fun nowMillis(): Long = origin.elapsedNow().inWholeMilliseconds
}
