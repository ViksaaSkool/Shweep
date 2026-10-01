package com.skooldev.shweep.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PurchaseManagerTest {

    private class FakeGateway : StorePurchaseGateway {
        lateinit var listener: StorePurchaseListener
        val requestedProducts = mutableListOf<String>()
        val restoreRequested = mutableListOf<String>()

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

        override fun restorePurchases(entitlementId: String) {
            restoreRequested += entitlementId
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

        manager.restore(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        assertTrue(gateway.restoreRequested.contains(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT))
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

        manager.restore(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        gateway.listener.onRestoreCompleted(gateway.none())

        assertEquals("No purchase found", manager.state.value.restoreErrors[PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT])
    }

    @Test
    fun restoreFailurePreservesKnownEntitlements() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()
        gateway.listener.onEntitlementsChanged(
            gateway.entitlements(EntitlementState.PURCHASED, EntitlementState.NOT_PURCHASED)
        )

        manager.restore(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        gateway.listener.onRestoreFailed(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT, "network")

        assertTrue(manager.state.value.hasUnlimitedSheep)
        assertFalse(manager.state.value.hasColorfulSheep)
        assertEquals(PurchaseOperation.IDLE, manager.state.value.operation)
        assertEquals("network", manager.state.value.restoreErrors[PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT])
    }

    @Test
    fun restoreReportsSuccessOnlyForTargetEntitlement() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()
        gateway.listener.onEntitlementsChanged(
            gateway.entitlements(EntitlementState.PURCHASED, EntitlementState.NOT_PURCHASED)
        )

        manager.restore(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        gateway.listener.onRestoreCompleted(gateway.entitlements(
            EntitlementState.PURCHASED, EntitlementState.NOT_PURCHASED
        ))

        assertTrue(manager.state.value.hasUnlimitedSheep)
        assertFalse(manager.state.value.hasColorfulSheep)
        assertNull(manager.state.value.errorMessage)
    }

    @Test
    fun colorfulRestoreErrorAppearsOnlyUnderColorfulKey() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        // No purchases
        gateway.listener.onEntitlementsChanged(gateway.none())

        // Restore Colorful Sheep
        manager.restore(PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT)
        gateway.listener.onRestoreCompleted(gateway.none())

        // Error should only be under Colorful key
        assertEquals("No purchase found", manager.state.value.restoreErrors[PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT])
        assertNull(manager.state.value.restoreErrors[PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT])
    }

    @Test
    fun unlimitedRestoreErrorAppearsOnlyUnderUnlimitedKey() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        gateway.listener.onEntitlementsChanged(gateway.none())

        manager.restore(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        gateway.listener.onRestoreCompleted(gateway.none())

        assertEquals("No purchase found", manager.state.value.restoreErrors[PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT])
        assertNull(manager.state.value.restoreErrors[PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT])
    }

    @Test
    fun productLoadCompletionMarksMissingProductsUnavailable() {
        val gateway = FakeGateway()
        val manager = PurchaseManager(gateway)
        manager.start()

        gateway.listener.onProductsLoaded(
            mapOf(
                PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT to
                    PurchasableProduct(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT, "\$0.99")
            )
        )

        assertFalse(manager.state.value.isProductUnavailable(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT))
        assertTrue(manager.state.value.isProductUnavailable(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT))
    }
}
