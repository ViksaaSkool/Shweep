package com.skooldev.shweep.purchase

class IosStorePurchaseGateway : StorePurchaseGateway {

    private var listener: StorePurchaseListener? = null

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        // Report unavailable until StoreKit bridge is connected
        listener.onProductUnavailable()
    }

    override fun refreshEntitlement() {
        listener?.onEntitlementChanged(false)
    }

    override fun purchaseUnlimitedSheep() {
        listener?.onPurchaseFailed("Store not available")
    }

    override fun restorePurchases() {
        listener?.onRestoreCompleted(false)
    }

    override fun stop() {
        listener = null
    }
}
