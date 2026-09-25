package com.skooldev.shweep.purchase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the UI-facing [PurchaseState] derived from the gateway. Every feature reads its own
 * entitlement from the state; there is no shared "paid" flag.
 */
class PurchaseManager(
    private val gateway: StorePurchaseGateway
) {
    private val _state = MutableStateFlow(PurchaseState())
    val state: StateFlow<PurchaseState> = _state.asStateFlow()

    private val listener = object : StorePurchaseListener {
        override fun onProductsLoaded(products: Map<String, PurchasableProduct>) {
            _state.value = _state.value.copy(
                products = products,
                productsUnavailable = false,
                errorMessage = null
            )
        }

        override fun onProductsUnavailable() {
            _state.value = _state.value.copy(productsUnavailable = true)
        }

        override fun onEntitlementsChanged(entitlements: Map<String, EntitlementState>) {
            _state.value = _state.value.copy(
                entitlements = entitlements,
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                errorMessage = null
            )
        }

        override fun onPurchasePending(productId: String) {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.PURCHASING,
                pendingProductId = productId,
                errorMessage = null
            )
        }

        override fun onPurchaseCancelled(productId: String) {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                errorMessage = null
            )
        }

        override fun onPurchaseFailed(productId: String, message: String) {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                errorMessage = message
            )
        }

        override fun onRestoreCompleted(entitlements: Map<String, EntitlementState>) {
            val found = entitlements.values.any { it == EntitlementState.PURCHASED }
            _state.value = _state.value.copy(
                entitlements = entitlements,
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                errorMessage = if (found) null else NO_PURCHASE_FOUND_MESSAGE
            )
        }
    }

    fun start() {
        gateway.start(listener)
    }

    fun purchase(productId: String) {
        if (_state.value.operation != PurchaseOperation.IDLE) return
        _state.value = _state.value.copy(
            operation = PurchaseOperation.PURCHASING,
            pendingProductId = productId,
            errorMessage = null
        )
        gateway.purchase(productId)
    }

    fun restore() {
        if (_state.value.operation != PurchaseOperation.IDLE) return
        _state.value = _state.value.copy(
            operation = PurchaseOperation.RESTORING,
            errorMessage = null
        )
        gateway.restorePurchases()
    }

    fun refresh() {
        gateway.refreshEntitlements()
    }

    fun stop() {
        gateway.stop()
    }

    private companion object {
        const val NO_PURCHASE_FOUND_MESSAGE = "No purchase found"
    }
}
