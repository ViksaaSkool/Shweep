package com.skooldev.shweep

object AppLinks {

    const val WEB = "https://shweep.lol"
    const val PRIVACY_POLICY = "https://shweep.lol/privacy/"
    const val TERMS_OF_SERVICE = "https://shweep.lol/terms/"
    const val KO_FI = "https://ko-fi.com/skooldev"
    const val GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.skooldev.shweep"
    const val APP_STORE = "https://apps.apple.com/app/id0000000000"

    val inviteMessage: String
        get() = """
            Try Shweep! $WEB

            Android: $GOOGLE_PLAY
            iPhone/iPad: $APP_STORE
        """.trimIndent()
}
