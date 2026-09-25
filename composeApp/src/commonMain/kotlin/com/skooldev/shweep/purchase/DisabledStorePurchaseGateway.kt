package com.skooldev.shweep.purchase

/** No-op gateway used when monetization is disabled. */
class DisabledStorePurchaseGateway : StorePurchaseGateway {
    override fun start(listener: StorePurchaseListener) {}
    override fun refreshEntitlements() {}
    override fun purchase(productId: String) {}
    override fun restorePurchases() {}
    override fun stop() {}
}
