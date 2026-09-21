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
 * TODO(manual setup): replace the two placeholder keys with the real public SDK keys from
 * https://app.revenuecat.com/ once the RevenueCat project and store products exist.
 */
object RevenueCatConfig {
    const val ANDROID_SDK_KEY = "goog_REPLACE_WITH_REVENUECAT_ANDROID_KEY"
    const val IOS_SDK_KEY = "appl_REPLACE_WITH_REVENUECAT_IOS_KEY"

    const val ENTITLEMENT_ID = "unlimited_sheep"
    const val PRODUCT_ID = UNLIMITED_SHEEP_PRODUCT_ID
    const val PAYWALL_SHOWN_AT_ATTRIBUTE = "paywall_shown_at"
}
