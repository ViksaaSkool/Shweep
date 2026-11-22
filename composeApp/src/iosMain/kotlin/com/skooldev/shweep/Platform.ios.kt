package com.skooldev.shweep

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val screenTransitionStyle: ScreenTransitionStyle =
        ScreenTransitionStyle.IOS

    override val prefersReducedMotion: Boolean = UIAccessibilityIsReduceMotionEnabled()

    override val appVersion: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: "unknown"

    override val appBuild: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
            ?: "unknown"
}

actual fun getPlatform(): Platform = IOSPlatform()
