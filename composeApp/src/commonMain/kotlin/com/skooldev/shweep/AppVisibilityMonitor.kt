package com.skooldev.shweep

import kotlinx.coroutines.flow.Flow

enum class AppVisibilityEvent {
    Foreground,
    Background
}

interface AppVisibilityMonitor {
    val events: Flow<AppVisibilityEvent>
}
