package com.skooldev.shweep

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

class IosAppVisibilityMonitor : AppVisibilityMonitor {
    override val events: Flow<AppVisibilityEvent> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter

        val bgObserver = center.addObserverForName(
            UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = null
        ) {
            trySend(AppVisibilityEvent.Background)
        }

        val fgObserver = center.addObserverForName(
            UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = null
        ) {
            trySend(AppVisibilityEvent.Foreground)
        }

        awaitClose {
            center.removeObserver(bgObserver)
            center.removeObserver(fgObserver)
        }
    }
}
