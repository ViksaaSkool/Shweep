package com.skooldev.shweep.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PurchaseManagerTest {

    private class FakeGateway : StorePurchaseGateway {
        lateinit var listener: StorePurchaseListener
        val requestedProducts = mutableListOf<String>()
        var restoreRequested = false

        override fun start(listener: StorePurchaseListener) {
            this.listener = listener
            listener.onProductsLoaded(
                mapOf(
                    PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT to
                        PurchasableProduct(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT, "\$0.99"),
                    PurchaseCatalog.COLORFUL_SHEEP_PRODUCT to
                        PurchasableProduct(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT, "\$0.99")
                )
            )
            listener.onEntitlementsChanged(none())
        }

        override fun refreshEntitlements() {}

        override fun purchase(productId: String) {
            requestedProducts += productId
        }

        override fun restorePurchases() {
            restoreRequested = true
        }

        override fun stop() {}

        fun none(): Map<String, EntitlementState> = mapOf(
            PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT to EntitlementState.NOT_PURCHASED,
            PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT to EntitlementState.NOT_PURCHASED
        )

        fun entitlements(
            unlimited: EntitlementState,
            colorful: EntitlementState
        ): Map<String, EntitlementState> = mapOf(
            PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT to unlimited,
            PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT to colorful
        )
    }

    @Test
    fun purchaseSuccessUnlocksOnlyTheTargetedEntitlement() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.purchase(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT)
        assertEquals(listOf(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT), gateway.requestedProducts)

        gateway.listener.onEntitlementsChanged(
            gateway.entitlements(EntitlementState.NOT_PURCHASED, EntitlementState.PURCHASED)
        )

        assertTrue(manager.state.value.hasColorfulSheep)
        assertFalse(manager.state.value.hasUnlimitedSheep)
        assertEquals(PurchaseOperation.IDLE, manager.state.value.operation)
    }

    @Test
    fun purchaseCancelReturnsToIdleWithoutUnlocking() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.purchase(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)
        gateway.listener.onPurchaseCancelled(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)

        assertEquals(PurchaseOperation.IDLE, manager.state.value.operation)
        assertFalse(manager.state.value.hasUnlimitedSheep)
        assertEquals(null, manager.state.value.errorMessage)
    }

    @Test
    fun purchaseFailureSurfacesMessage() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.purchase(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)
        gateway.listener.onPurchaseFailed(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT, "declined")

        assertEquals(PurchaseOperation.IDLE, manager.state.value.operation)
        assertEquals("declined", manager.state.value.errorMessage)
        assertFalse(manager.state.value.hasUnlimitedSheep)
    }

    @Test
    fun pendingPurchaseKeepsProductId() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.purchase(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)
        gateway.listener.onPurchasePending(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)

        assertEquals(PurchaseOperation.PURCHASING, manager.state.value.operation)
        assertEquals(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT, manager.state.value.pendingProductId)
    }

    @Test
    fun restoreReflectsEveryOwnedEntitlement() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.restore()
        assertTrue(gateway.restoreRequested)
        gateway.listener.onRestoreCompleted(
            gateway.entitlements(EntitlementState.PURCHASED, EntitlementState.PURCHASED)
        )

        assertTrue(manager.state.value.hasUnlimitedSheep)
        assertTrue(manager.state.value.hasColorfulSheep)
    }

    @Test
    fun restoreWithNoPurchasesReportsIt() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        manager.restore()
        gateway.listener.onRestoreCompleted(gateway.none())

        assertEquals("No purchase found", manager.state.value.errorMessage)
    }
}
