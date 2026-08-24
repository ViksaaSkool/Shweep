package com.skooldev.shweep.purchase

interface StorePurchaseListener {
    fun onProductLoaded(localizedPrice: String)
    fun onProductUnavailable()
    fun onEntitlementChanged(isPurchased: Boolean)
    fun onPurchasePending()
    fun onPurchaseCancelled()
    fun onPurchaseFailed(message: String)
    fun onRestoreCompleted(isPurchased: Boolean)
}

interface StorePurchaseGateway {
    fun start(listener: StorePurchaseListener)
    fun refreshEntitlement()
    fun purchaseUnlimitedSheep()
    fun restorePurchases()
    fun stop()
}
