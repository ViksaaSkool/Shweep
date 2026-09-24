package com.skooldev.shweep.purchase

class DisabledStorePurchaseGateway : StorePurchaseGateway {
    override fun start(listener: StorePurchaseListener) {}
    override fun refreshEntitlement() {}
    override fun purchaseUnlimitedSheep() {}
    override fun restorePurchases() {}
    override fun stop() {}
}
