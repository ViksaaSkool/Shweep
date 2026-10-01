package com.skooldev.shweep

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val appVersion: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: "unknown"

    override val appBuild: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
            ?: "unknown"
}

actual fun getPlatform(): Platform = IOSPlatform()
