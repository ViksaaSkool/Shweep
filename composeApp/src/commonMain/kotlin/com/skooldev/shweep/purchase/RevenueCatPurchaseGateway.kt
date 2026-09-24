package com.skooldev.shweep.purchase

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitGetProducts
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * RevenueCat-backed purchase gateway. One implementation serves Android and iOS because the
 * purchases-kmp SDK wraps Google Play Billing and StoreKit behind a common API.
 *
 * The app is anonymous (per device): no app user id is passed, so RevenueCat generates and stores
 * an anonymous id on the device. Purchase status is read from the `unlimited_sheep` entitlement.
 */
class RevenueCatPurchaseGateway(
    private val apiKey: String
) : StorePurchaseGateway {

    private var listener: StorePurchaseListener? = null
    private var scope: CoroutineScope? = null
    private var product: StoreProduct? = null

    private val delegate = object : PurchasesDelegate {
        override fun onPurchasePromoProduct(
            product: StoreProduct,
            startPurchase: (
                onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
            ) -> Unit
        ) {
            // Promotional App Store purchases are not used by Shweep.
        }

        override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
            listener?.onEntitlementChanged(isPurchased(customerInfo))
        }
    }

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        scope?.cancel()
        val activeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = activeScope

        ensureConfigured()
        Purchases.sharedInstance.delegate = delegate
        activeScope.launch { loadProductAndEntitlement() }
    }

    override fun refreshEntitlement() {
        val activeScope = scope ?: return
        if (!Purchases.isConfigured) return

        activeScope.launch {
            try {
                listener?.onEntitlementChanged(isPurchased(Purchases.sharedInstance.awaitCustomerInfo()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: PurchasesException) {
                // Keep the last known entitlement; a transient network failure must not revoke access.
            }
        }
    }

    override fun purchaseUnlimitedSheep() {
        val storeProduct = product
        if (storeProduct == null) {
            listener?.onPurchaseFailed(PURCHASE_FAILED_MESSAGE)
            return
        }
        val activeScope = scope ?: return

        activeScope.launch {
            try {
                val purchase = Purchases.sharedInstance.awaitPurchase(storeProduct)
                if (isPurchased(purchase.customerInfo)) {
                    listener?.onEntitlementChanged(true)
                } else {
                    // Deferred payment (for example Google Play pending transactions).
                    listener?.onPurchasePending()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (transaction: PurchasesTransactionException) {
                if (transaction.userCancelled) {
                    listener?.onPurchaseCancelled()
                } else {
                    listener?.onPurchaseFailed(transaction.message)
                }
            } catch (_: PurchasesException) {
                listener?.onPurchaseFailed(PURCHASE_FAILED_MESSAGE)
            }
        }
    }

    override fun restorePurchases() {
        val activeScope = scope ?: return

        activeScope.launch {
            try {
                listener?.onRestoreCompleted(isPurchased(Purchases.sharedInstance.awaitRestore()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: PurchasesException) {
                listener?.onRestoreCompleted(false)
            }
        }
    }

    override fun stop() {
        if (Purchases.isConfigured) {
            Purchases.sharedInstance.delegate = null
        }
        scope?.cancel()
        scope = null
        product = null
        listener = null
    }

    private fun ensureConfigured() {
        if (!Purchases.isConfigured) {
            Purchases.configure(apiKey = apiKey)
        }
    }

    private suspend fun loadProductAndEntitlement() {
        try {
            listener?.onEntitlementChanged(isPurchased(Purchases.sharedInstance.awaitCustomerInfo()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: PurchasesException) {
            listener?.onEntitlementChanged(false)
        }

        try {
            val storeProduct = findProductInOfferings(Purchases.sharedInstance.awaitOfferings())
                ?: Purchases.sharedInstance.awaitGetProducts(listOf(RevenueCatConfig.PRODUCT_ID))
                    .firstOrNull()

            if (storeProduct != null) {
                product = storeProduct
                listener?.onProductLoaded(storeProduct.price.formatted)
            } else {
                listener?.onProductUnavailable()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: PurchasesException) {
            listener?.onProductUnavailable()
        }
    }

    private fun findProductInOfferings(offerings: Offerings): StoreProduct? {
        offerings.all.values.forEach { offering ->
            offering.availablePackages.forEach { pkg ->
                if (pkg.storeProduct.id == RevenueCatConfig.PRODUCT_ID) {
                    return pkg.storeProduct
                }
            }
        }
        return null
    }

    private fun isPurchased(customerInfo: CustomerInfo): Boolean =
        customerInfo.entitlements[RevenueCatConfig.ENTITLEMENT_ID]?.isActive == true

    private companion object {
        const val PURCHASE_FAILED_MESSAGE = "Purchase could not be completed"
    }
}
