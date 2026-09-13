package com.skooldev.shweep.monetization

/**
 * Entry point for the optional monetization stack (RevenueCat purchases +
 * Firebase-backed cross-device sheep quota).
 *
 * This module is excluded from the default build. See `MONETIZATION.md` for the
 * activation runbook and the required configuration.
 */
object MonetizationBootstrap {

    /** RevenueCat entitlement that grants unlimited sheep across platforms. */
    const val UNLIMITED_SHEEP_ENTITLEMENT_ID = "unlimited_sheep"

    /** Store product / RevenueCat package identifier. */
    const val UNLIMITED_SHEEP_PRODUCT_ID = "unlimited_sheep"

    /** Daily sheep allowance enforced by the backend when not entitled. */
    const val DAILY_SHEEP_LIMIT = 50

    /** Quota reset boundary as an hour in UTC (12:00 UTC). */
    const val QUOTA_RESET_HOUR_UTC = 12
}
