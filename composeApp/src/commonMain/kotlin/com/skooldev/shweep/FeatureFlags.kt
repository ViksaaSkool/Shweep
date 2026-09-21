package com.skooldev.shweep

object FeatureFlags {
    const val LIMITED_DAILY_SHEEP_ENABLED = false

    /**
     * Local-only test mode. When true, the sheep allowance becomes 3 sheep / 3 minutes and the
     * purchase flow uses an in-memory mock gateway (instant purchase, no RevenueCat keys needed).
     * Flip it locally while testing and keep it false in commits.
     */
    const val LOCAL_TEST_MODE = true
}
