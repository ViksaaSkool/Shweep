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
                productsLoadCompleted = true,
                errorMessage = null
            )
        }

        override fun onProductsUnavailable() {
            _state.value = _state.value.copy(
                productsUnavailable = true,
                productsLoadCompleted = true
            )
        }

        override fun onEntitlementsChanged(entitlements: Map<String, EntitlementState>) {
            _state.value = _state.value.copy(
                entitlements = entitlements,
                operation = PurchaseOperation.IDLE,
                pendingProductId = null
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
            val targetEntitlement = _state.value.pendingRestoreEntitlementId
            val found = if (targetEntitlement != null) {
                entitlements[targetEntitlement] == EntitlementState.PURCHASED
            } else {
                entitlements.values.any { it == EntitlementState.PURCHASED }
            }
            val newRestoreErrors = _state.value.restoreErrors.toMutableMap()
            if (targetEntitlement != null) {
                if (found) {
                    newRestoreErrors.remove(targetEntitlement)
                } else {
                    newRestoreErrors[targetEntitlement] = NO_PURCHASE_FOUND_MESSAGE
                }
            }
            _state.value = _state.value.copy(
                entitlements = entitlements,
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                pendingRestoreEntitlementId = null,
                restoreErrors = newRestoreErrors
            )
        }

        override fun onRestoreFailed(productId: String, message: String) {
            val targetEntitlement = _state.value.pendingRestoreEntitlementId
            val newRestoreErrors = _state.value.restoreErrors.toMutableMap()
            targetEntitlement?.let { newRestoreErrors[it] = message }
            // Keep the last known entitlements; a transient restore failure must not revoke access.
            _state.value = _state.value.copy(
                operation = PurchaseOperation.IDLE,
                pendingProductId = null,
                pendingRestoreEntitlementId = null,
                restoreErrors = newRestoreErrors
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

    fun restore(entitlementId: String) {
        if (_state.value.operation != PurchaseOperation.IDLE) return
        _state.value = _state.value.copy(
            operation = PurchaseOperation.RESTORING,
            pendingRestoreEntitlementId = entitlementId,
            restoreErrors = _state.value.restoreErrors.toMutableMap().also { it.remove(entitlementId) }
        )
        gateway.restorePurchases(entitlementId)
    }

    /** Clears all per-entitlement restore error messages. */
    fun clearRestoreErrors() {
        if (_state.value.restoreErrors.isNotEmpty()) {
            _state.value = _state.value.copy(restoreErrors = emptyMap())
        }
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
