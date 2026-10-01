package com.skooldev.shweep.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PurchaseStateTest {

    private fun entitlements(
        unlimited: EntitlementState,
        colorful: EntitlementState
    ): Map<String, EntitlementState> = mapOf(
        PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT to unlimited,
        PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT to colorful
    )

    @Test
    fun entitlementsAreIndependent() {
        val cases = listOf(
            Triple(EntitlementState.NOT_PURCHASED, EntitlementState.NOT_PURCHASED, false to false),
            Triple(EntitlementState.PURCHASED, EntitlementState.NOT_PURCHASED, true to false),
            Triple(EntitlementState.NOT_PURCHASED, EntitlementState.PURCHASED, false to true),
            Triple(EntitlementState.PURCHASED, EntitlementState.PURCHASED, true to true)
        )

        cases.forEach { (unlimited, colorful, expected) ->
            val state = PurchaseState(entitlements = entitlements(unlimited, colorful))
            assertEquals(expected.first, state.hasUnlimitedSheep)
            assertEquals(expected.second, state.hasColorfulSheep)
        }
    }

    @Test
    fun missingEntitlementsDefaultToChecking() {
        val state = PurchaseState()
        assertEquals(
            EntitlementState.CHECKING,
            state.entitlementState(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
        )
        assertFalse(state.hasUnlimitedSheep)
        assertFalse(state.hasColorfulSheep)
    }

    @Test
    fun canBuyRequiresNotPurchasedLoadedProductAndIdle() {
        val product = PurchasableProduct(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT, "\$0.99")
        val base = PurchaseState(
            entitlements = entitlements(EntitlementState.NOT_PURCHASED, EntitlementState.NOT_PURCHASED),
            products = mapOf(product.productId to product)
        )

        assertTrue(base.canBuy(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT, product.productId))
        // Other product is not loaded.
        assertFalse(base.canBuy(PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT, PurchaseCatalog.COLORFUL_SHEEP_PRODUCT))
        // Busy with another operation.
        assertFalse(
            base.copy(operation = PurchaseOperation.PURCHASING)
                .canBuy(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT, product.productId)
        )
        // Already purchased.
        assertFalse(
            base.copy(entitlements = entitlements(EntitlementState.PURCHASED, EntitlementState.NOT_PURCHASED))
                .canBuy(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT, product.productId)
        )
    }

    @Test
    fun entitlementForMapsProductsToEntitlements() {
        assertEquals(
            PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT,
            PurchaseCatalog.entitlementFor(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)
        )
        assertEquals(
            PurchaseCatalog.COLORFUL_SHEEP_ENTITLEMENT,
            PurchaseCatalog.entitlementFor(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT)
        )
        assertNull(PurchaseCatalog.entitlementFor("unknown_product"))
    }
}
