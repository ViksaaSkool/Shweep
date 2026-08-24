package com.skooldev.shweep.purchase

const val UNLIMITED_SHEEP_PRODUCT_ID = "unlimited_sheep"

data class UnlimitedSheepProduct(
    val productId: String,
    val localizedPrice: String
)

data class UnlimitedSheepPurchaseState(
    val entitlement: EntitlementState = EntitlementState.CHECKING,
    val product: UnlimitedSheepProduct? = null,
    val operation: PurchaseOperation = PurchaseOperation.IDLE,
    val errorMessage: String? = null
) {
    val isPurchased: Boolean get() = entitlement == EntitlementState.PURCHASED
    val isProductLoaded: Boolean get() = product != null
    val canBuy: Boolean get() = entitlement == EntitlementState.NOT_PURCHASED && isProductLoaded
}

enum class EntitlementState {
    CHECKING,
    NOT_PURCHASED,
    PURCHASED,
    UNAVAILABLE
}

enum class PurchaseOperation {
    IDLE,
    PURCHASING,
    RESTORING
}
