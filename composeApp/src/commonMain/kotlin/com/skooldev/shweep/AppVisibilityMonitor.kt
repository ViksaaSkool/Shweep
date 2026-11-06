package com.skooldev.shweep

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class AppVisibilityEvent {
    Foreground,
    Background
}

interface AppVisibilityMonitor {
    val events: Flow<AppVisibilityEvent>
}

object NoOpVisibilityMonitor : AppVisibilityMonitor {
    override val events: Flow<AppVisibilityEvent> = emptyFlow()
}
