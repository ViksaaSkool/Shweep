package com.skooldev.shweep

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidAppVisibilityMonitor : AppVisibilityMonitor {
    override val events: Flow<AppVisibilityEvent> = callbackFlow {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                trySend(AppVisibilityEvent.Foreground)
            }

            override fun onStop(owner: LifecycleOwner) {
                trySend(AppVisibilityEvent.Background)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        awaitClose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
}
