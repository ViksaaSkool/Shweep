package com.skooldev.shweep.purchase

/**
 * Central catalog of independently purchasable features.
 *
 * RevenueCat is the source of truth for ownership. Three kinds of identifiers are deliberately
 * kept distinct so the layers can evolve independently:
 *
 *  - `*_ENTITLEMENT`: RevenueCat entitlement identifiers (what the customer owns).
 *  - `*_PRODUCT`: store product identifiers (Google Play / App Store).
 *  - package identifiers live in the RevenueCat dashboard and are resolved at runtime.
 *
 * Adding a new paid feature means adding an entitlement, a product, and a feature gate — not
 * redesigning the purchase system.
 */
object  PurchaseCatalog {

    // RevenueCat entitlement identifiers.
    const val UNLIMITED_SHEEP_ENTITLEMENT = "unlimited_sheep"
    const val COLORFUL_SHEEP_ENTITLEMENT = "colorful_sheep"

    // Store product identifiers (Google Play / App Store).
    const val UNLIMITED_SHEEP_PRODUCT = "unlimited_sheep"
    const val COLORFUL_SHEEP_PRODUCT = "colorful_sheep"

    val entitlementIds: List<String> = listOf(
        UNLIMITED_SHEEP_ENTITLEMENT,
        COLORFUL_SHEEP_ENTITLEMENT
    )

    val productIds: List<String> = listOf(
        UNLIMITED_SHEEP_PRODUCT,
        COLORFUL_SHEEP_PRODUCT
    )

    /** The entitlement a given store product is expected to unlock. */
    fun entitlementFor(productId: String): String? = when (productId) {
        UNLIMITED_SHEEP_PRODUCT -> UNLIMITED_SHEEP_ENTITLEMENT
        COLORFUL_SHEEP_PRODUCT -> COLORFUL_SHEEP_ENTITLEMENT
        else -> null
    }
}
