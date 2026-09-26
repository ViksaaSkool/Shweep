package com.skooldev.shweep.purchase

/**
 * In-memory purchase gateway used by previews and local test mode. Ownership is a simple set of
 * owned product ids; entitlements are derived independently, so any combination is representable.
 */
class MockStorePurchaseGateway(
    initialOwned: Set<String> = emptySet(),
    private val prices: Map<String, String> = DEFAULT_PRICES
) : StorePurchaseGateway {

    private var listener: StorePurchaseListener? = null
    private val owned = initialOwned.toMutableSet()

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        listener.onProductsLoaded(
            prices.mapValues { (productId, price) -> PurchasableProduct(productId, price) }
        )
        listener.onEntitlementsChanged(entitlements())
    }

    override fun refreshEntitlements() {
        listener?.onEntitlementsChanged(entitlements())
    }

    override fun purchase(productId: String) {
        if (!prices.containsKey(productId)) {
            listener?.onPurchaseFailed(productId, "Unknown product")
            return
        }
        listener?.onPurchasePending(productId)
        owned += productId
        listener?.onEntitlementsChanged(entitlements())
    }

    override fun restorePurchases(entitlementId: String) {
        listener?.onRestoreCompleted(entitlements())
    }

    override fun stop() {
        listener = null
    }

    private fun entitlements(): Map<String, EntitlementState> = mapOf(
        PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT to stateFor(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT),
        PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT to stateFor(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT)
    )

    private fun stateFor(productId: String): EntitlementState =
        if (owned.contains(productId)) EntitlementState.PURCHASED else EntitlementState.NOT_PURCHASED

    private companion object {
        val DEFAULT_PRICES = mapOf(
            PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT to "\$0.99",
            PurchaseCatalog.COLORFUL_SHEEP_PRODUCT to "\$0.99"
        )
    }
}
