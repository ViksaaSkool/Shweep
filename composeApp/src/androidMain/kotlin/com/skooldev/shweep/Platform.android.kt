package com.skooldev.shweep

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val appVersion: String
    override val appBuild: String

    init {
        val packageInfo = runCatching {
            val context = ShweepApplication.instance
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        appVersion = packageInfo?.versionName ?: "unknown"
        appBuild = packageInfo?.let { info ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toString()
            }
        } ?: "unknown"
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
