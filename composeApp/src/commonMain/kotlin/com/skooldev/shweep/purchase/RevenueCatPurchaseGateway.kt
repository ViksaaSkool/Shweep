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
 * an anonymous id on the device. Every entitlement in [PurchaseCatalog] is read independently from
 * [CustomerInfo], so a customer can own any combination of products.
 */
class RevenueCatPurchaseGateway(
    private val apiKey: String
) : StorePurchaseGateway {

    private var listener: StorePurchaseListener? = null
    private var scope: CoroutineScope? = null
    private val products = mutableMapOf<String, StoreProduct>()

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
            listener?.onEntitlementsChanged(entitlementsOf(customerInfo))
        }
    }

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        scope?.cancel()
        val activeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = activeScope

        ensureConfigured()
        Purchases.sharedInstance.delegate = delegate
        activeScope.launch { loadProductsAndEntitlements() }
    }

    override fun refreshEntitlements() {
        val activeScope = scope ?: return
        if (!Purchases.isConfigured) return

        activeScope.launch {
            try {
                listener?.onEntitlementsChanged(
                    entitlementsOf(Purchases.sharedInstance.awaitCustomerInfo())
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: PurchasesException) {
                // Keep the last known entitlements; a transient network failure must not revoke access.
            }
        }
    }

    override fun purchase(productId: String) {
        val storeProduct = products[productId]
        if (storeProduct == null) {
            listener?.onPurchaseFailed(productId, PURCHASE_FAILED_MESSAGE)
            return
        }
        val activeScope = scope ?: return

        activeScope.launch {
            try {
                val purchase = Purchases.sharedInstance.awaitPurchase(storeProduct)
                val entitlements = entitlementsOf(purchase.customerInfo)
                val target = PurchaseCatalog.entitlementFor(productId)
                if (target != null && entitlements[target] == EntitlementState.PURCHASED) {
                    listener?.onEntitlementsChanged(entitlements)
                } else {
                    // Deferred payment (for example Google Play pending transactions).
                    listener?.onPurchasePending(productId)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (transaction: PurchasesTransactionException) {
                if (transaction.userCancelled) {
                    listener?.onPurchaseCancelled(productId)
                } else {
                    listener?.onPurchaseFailed(productId, transaction.message)
                }
            } catch (_: PurchasesException) {
                listener?.onPurchaseFailed(productId, PURCHASE_FAILED_MESSAGE)
            }
        }
    }

    override fun restorePurchases(entitlementId: String) {
        val activeScope = scope ?: return

        activeScope.launch {
            try {
                listener?.onRestoreCompleted(entitlementsOf(Purchases.sharedInstance.awaitRestore()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: PurchasesException) {
                // Do not report an empty ownership map: a failed restore must not revoke access.
                listener?.onRestoreFailed(entitlementId, RESTORE_FAILED_MESSAGE)
            }
        }
    }

    override fun stop() {
        if (Purchases.isConfigured) {
            Purchases.sharedInstance.delegate = null
        }
        scope?.cancel()
        scope = null
        products.clear()
        listener = null
    }

    private fun ensureConfigured() {
        if (!Purchases.isConfigured) {
            Purchases.configure(apiKey = apiKey)
        }
    }

    private suspend fun loadProductsAndEntitlements() {
        try {
            listener?.onEntitlementsChanged(
                entitlementsOf(Purchases.sharedInstance.awaitCustomerInfo())
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: PurchasesException) {
            // Leave entitlements unknown rather than reporting them as not purchased. Counting then
            // falls back to the local allowance until RevenueCat can confirm ownership.
        }

        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val resolved = mutableMapOf<String, PurchasableProduct>()
            PurchaseCatalog.productIds.forEach { productId ->
                val storeProduct = findProductInOfferings(offerings, productId)
                    ?: Purchases.sharedInstance.awaitGetProducts(listOf(productId)).firstOrNull()
                if (storeProduct != null) {
                    products[productId] = storeProduct
                    resolved[productId] = PurchasableProduct(productId, storeProduct.price.formatted)
                }
            }
            if (resolved.isEmpty()) {
                listener?.onProductsUnavailable()
            } else {
                listener?.onProductsLoaded(resolved)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: PurchasesException) {
            listener?.onProductsUnavailable()
        }
    }

    private fun findProductInOfferings(offerings: Offerings, productId: String): StoreProduct? {
        offerings.all.values.forEach { offering ->
            offering.availablePackages.forEach { pkg ->
                if (pkg.storeProduct.id == productId) {
                    return pkg.storeProduct
                }
            }
        }
        return null
    }

    private fun entitlementsOf(customerInfo: CustomerInfo): Map<String, EntitlementState> =
        PurchaseCatalog.entitlementIds.associateWith { entitlementId ->
            if (customerInfo.entitlements[entitlementId]?.isActive == true) {
                EntitlementState.PURCHASED
            } else {
                EntitlementState.NOT_PURCHASED
            }
        }

    private companion object {
        const val PURCHASE_FAILED_MESSAGE = "Purchase could not be completed"
        const val RESTORE_FAILED_MESSAGE = "Restore could not be completed"
    }
}
