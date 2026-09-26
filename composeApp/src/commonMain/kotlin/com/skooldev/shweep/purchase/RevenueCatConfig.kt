package com.skooldev.shweep.purchase

/**
 * RevenueCat configuration for Shweep.
 *
 * The SDK keys below are *public* RevenueCat API keys. They are designed to be embedded in the
 * shipped app and are not secrets; every installed copy contains them. The platform entry points
 * select the matching key (Google Play for Android, App Store for iOS).
 *
 * The RevenueCat *secret* API key, the Google Play service-account JSON, and the App Store Connect
 * `.p8` key must never be committed here. They live only in the RevenueCat dashboard.
 *
 * Entitlement and product identifiers live in [PurchaseCatalog], not here.
 */
object RevenueCatConfig {
    /** Test Store key for local debug builds (Android and iOS). */
    const val DEBUG_SDK_KEY = "test_lpINQsBjwXqLswXwaeZhaxPGuwZ"

    /** Android production key from the RevenueCat dashboard. */
    const val ANDROID_SDK_KEY = "goog_REPLACE_WITH_REVENUECAT_ANDROID_KEY"

    /** iOS production key from the RevenueCat dashboard. */
    const val IOS_SDK_KEY = "appl_REPLACE_WITH_REVENUECAT_IOS_KEY"

    /** Returns the Android SDK key: test key for debug builds, production key for release. */
    fun androidSdkKey(isDebug: Boolean): String =
        if (isDebug) DEBUG_SDK_KEY else ANDROID_SDK_KEY

    /** Returns the iOS SDK key: test key for debug builds, production key for release. */
    fun iosSdkKey(isDebug: Boolean): String =
        if (isDebug) DEBUG_SDK_KEY else IOS_SDK_KEY
}
