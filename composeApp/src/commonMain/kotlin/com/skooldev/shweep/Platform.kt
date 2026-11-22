package com.skooldev.shweep

enum class ScreenTransitionStyle {
    ANDROID,
    IOS
}

interface Platform {
    val name: String

    /** Marketing version of the installed app, for example "1.0.2". */
    val appVersion: String

    /** Build number of the installed app, for example "12". */
    val appBuild: String

    /** Motion used for navigation, chosen to match the host platform. */
    val screenTransitionStyle: ScreenTransitionStyle

    /** True when the system asks apps to reduce or remove motion. */
    val prefersReducedMotion: Boolean
}

expect fun getPlatform(): Platform

expect fun shareText(text: String, title: String)
