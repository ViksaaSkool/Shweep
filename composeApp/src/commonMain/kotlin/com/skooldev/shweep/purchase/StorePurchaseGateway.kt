package com.skooldev.shweep.purchase

interface StorePurchaseListener {
    fun onProductsLoaded(products: Map<String, PurchasableProduct>)
    fun onProductsUnavailable()
    fun onEntitlementsChanged(entitlements: Map<String, EntitlementState>)
    fun onPurchasePending(productId: String)
    fun onPurchaseCancelled(productId: String)
    fun onPurchaseFailed(productId: String, message: String)
    fun onRestoreCompleted(entitlements: Map<String, EntitlementState>)
    fun onRestoreFailed(productId: String, message: String)
}

/**
 * Platform purchase surface. Entitlements are keyed by RevenueCat entitlement id and products by
 * store product id, so multiple independent features share one gateway.
 */
interface StorePurchaseGateway {
    fun start(listener: StorePurchaseListener)
    fun refreshEntitlements()
    fun purchase(productId: String)
    fun restorePurchases(entitlementId: String)
    fun stop()
}
