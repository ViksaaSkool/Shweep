package com.skooldev.shweep

object FeatureFlags {
    const val LIMITED_DAILY_SHEEP_ENABLED = true

    /**
     * The app version whose first launch shows the one-time "what's changed" notice. Bump this only
     * when a release introduces user-visible changes that returning users should be told about.
     */
    const val UPDATE_NOTICE_VERSION = "2.0.0"

    /**
     * Local-only test mode. When true, the sheep allowance becomes 3 sheep / 3 minutes and the
     * purchase flow uses an in-memory mock gateway (instant purchase, no RevenueCat keys needed).
     * Flip it locally while testing and keep it false in commits.
     */
    const val LOCAL_TEST_MODE = true
}
