package com.skooldev.shweep.purchase

/** A store product resolved from RevenueCat, with its localized price. */
data class PurchasableProduct(
    val productId: String,
    val localizedPrice: String
)

/** Ownership status of a single RevenueCat entitlement. */
enum class EntitlementState {
    CHECKING,
    NOT_PURCHASED,
    PURCHASED
}

enum class PurchaseOperation {
    IDLE,
    PURCHASING,
    RESTORING
}

/**
 * UI-facing cache of RevenueCat's [com.revenuecat.purchases.kmp.models.CustomerInfo].
 *
 * This is not an authority: every feature derives its own access from the relevant entitlement
 * ([hasUnlimitedSheep], [hasColorfulSheep]). There is intentionally no single "pro"/"paid" flag.
 */
data class PurchaseState(
    val entitlements: Map<String, EntitlementState> = emptyMap(),
    val products: Map<String, PurchasableProduct> = emptyMap(),
    val productsUnavailable: Boolean = false,
    val operation: PurchaseOperation = PurchaseOperation.IDLE,
    val pendingProductId: String? = null,
    val errorMessage: String? = null
) {
    fun entitlementState(entitlementId: String): EntitlementState =
        entitlements[entitlementId] ?: EntitlementState.CHECKING

    val hasUnlimitedSheep: Boolean
        get() = entitlementState(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT) == EntitlementState.PURCHASED

    val hasColorfulSheep: Boolean
        get() = entitlementState(PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT) == EntitlementState.PURCHASED

    fun product(productId: String): PurchasableProduct? = products[productId]

    fun isProductLoaded(productId: String): Boolean = products.containsKey(productId)

    fun isPurchased(entitlementId: String): Boolean =
        entitlementState(entitlementId) == EntitlementState.PURCHASED

    fun canBuy(entitlementId: String, productId: String): Boolean =
        entitlementState(entitlementId) == EntitlementState.NOT_PURCHASED &&
            isProductLoaded(productId) &&
            operation == PurchaseOperation.IDLE
}
