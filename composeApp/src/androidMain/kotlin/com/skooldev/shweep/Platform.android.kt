package com.skooldev.shweep

import android.os.Build
import android.provider.Settings
import com.skooldev.shweep.ShweepApplication

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val appVersion: String
    override val appBuild: String
    override val screenTransitionStyle: ScreenTransitionStyle =
        ScreenTransitionStyle.ANDROID
    override val prefersReducedMotion: Boolean = runCatching {
        Settings.Global.getFloat(
            ShweepApplication.instance.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)

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
