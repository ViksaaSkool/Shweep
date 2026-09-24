package com.skooldev.shweep.purchase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnlimitedSheepPurchaseManager(
    private val gateway: StorePurchaseGateway
) {
    private val _state = MutableStateFlow(UnlimitedSheepPurchaseState())
    val state: StateFlow<UnlimitedSheepPurchaseState> = _state.asStateFlow()

    private val listener = object : StorePurchaseListener {
        override fun onProductLoaded(localizedPrice: String) {
            _state.value = _state.value.copy(
                product = UnlimitedSheepProduct(
                    productId = UNLIMITED_SHEEP_PRODUCT_ID,
                    localizedPrice = localizedPrice
                ),
                errorMessage = null
            )
        }

        override fun onProductUnavailable() {
            _state.value = _state.value.copy(
                entitlement = EntitlementState.UNAVAILABLE,
                errorMessage = null
            )
        }

        override fun onEntitlementChanged(isPurchased: Boolean) {
            _state.value = _state.value.copy(
                entitlement = if (isPurchased) EntitlementState.PURCHASED else EntitlementState.NOT_PURCHASED,
                operation = PurchaseOperation.IDLE,
                errorMessage = null
            )
        }

        override fun onPurchasePending() {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.PURCHASING,
                errorMessage = null
            )
        }

        override fun onPurchaseCancelled() {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.IDLE,
                errorMessage = null
            )
        }

        override fun onPurchaseFailed(message: String) {
            _state.value = _state.value.copy(
                operation = PurchaseOperation.IDLE,
                errorMessage = message
            )
        }

        override fun onRestoreCompleted(isPurchased: Boolean) {
            _state.value = _state.value.copy(
                entitlement = if (isPurchased) EntitlementState.PURCHASED else EntitlementState.NOT_PURCHASED,
                operation = PurchaseOperation.IDLE,
                errorMessage = if (!isPurchased) "No purchase found" else null
            )
        }
    }

    fun start() {
        gateway.start(listener)
    }

    fun purchase() {
        if (_state.value.operation != PurchaseOperation.IDLE) return
        _state.value = _state.value.copy(operation = PurchaseOperation.PURCHASING)
        gateway.purchaseUnlimitedSheep()
    }

    fun restore() {
        if (_state.value.operation != PurchaseOperation.IDLE) return
        _state.value = _state.value.copy(operation = PurchaseOperation.RESTORING)
        gateway.restorePurchases()
    }

    fun refresh() {
        gateway.refreshEntitlement()
    }

    fun stop() {
        gateway.stop()
    }
}
