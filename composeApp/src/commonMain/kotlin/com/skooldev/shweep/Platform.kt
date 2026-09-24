package com.skooldev.shweep

interface Platform {
    val name: String

    /** Marketing version of the installed app, for example "1.0.2". */
    val appVersion: String

    /** Build number of the installed app, for example "12". */
    val appBuild: String
}

expect fun getPlatform(): Platform

expect fun shareText(text: String, title: String)
