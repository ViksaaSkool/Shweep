package com.skooldev.shweep.purchase

class MockStorePurchaseGateway(
    private val initialPurchased: Boolean = false,
    private val price: String = "\$0.99"
) : StorePurchaseGateway {
    private var listener: StorePurchaseListener? = null
    private var purchased = initialPurchased

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        listener.onProductLoaded(price)
        listener.onEntitlementChanged(purchased)
    }

    override fun refreshEntitlement() {
        listener?.onEntitlementChanged(purchased)
    }

    override fun purchaseUnlimitedSheep() {
        listener?.onPurchasePending()
        purchased = true
        listener?.onEntitlementChanged(true)
    }

    override fun restorePurchases() {
        listener?.onRestoreCompleted(purchased)
    }

    override fun stop() {
        listener = null
    }
}
