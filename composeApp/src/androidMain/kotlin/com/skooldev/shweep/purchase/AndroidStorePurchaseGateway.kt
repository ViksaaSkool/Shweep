package com.skooldev.shweep.purchase

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AndroidStorePurchaseGateway(
    private val activity: Activity
) : StorePurchaseGateway {

    private var listener: StorePurchaseListener? = null
    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                listener?.onPurchaseCancelled()
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refreshEntitlement()
            }
            else -> {
                listener?.onPurchaseFailed(billingResult.debugMessage)
            }
        }
    }

    override fun start(listener: StorePurchaseListener) {
        this.listener = listener
        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        connectAndQuery()
    }

    override fun refreshEntitlement() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            val isPurchased = purchases.any {
                it.products.contains(UNLIMITED_SHEEP_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            listener?.onEntitlementChanged(isPurchased)
        }
    }

    override fun purchaseUnlimitedSheep() {
        val details = productDetails
        if (details == null) {
            listener?.onPurchaseFailed("Product not loaded")
            return
        }

        val productList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    override fun restorePurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            val isPurchased = purchases.any {
                it.products.contains(UNLIMITED_SHEEP_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            listener?.onRestoreCompleted(isPurchased)
        }
    }

    override fun stop() {
        scope.cancel()
        billingClient?.endConnection()
        billingClient = null
        listener = null
    }

    private fun connectAndQuery() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetails()
                        refreshEntitlement()
                    }
                } else {
                    listener?.onProductUnavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                listener?.onProductUnavailable()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(UNLIMITED_SHEEP_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        billingClient?.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
        ) { _, details ->
            val product = details.firstOrNull()
            if (product != null) {
                productDetails = product
                val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                if (price != null) {
                    listener?.onProductLoaded(price)
                } else {
                    listener?.onProductUnavailable()
                }
            } else {
                listener?.onProductUnavailable()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(UNLIMITED_SHEEP_PRODUCT_ID)) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshEntitlement()
                }
            }
        } else {
            refreshEntitlement()
        }
    }
}
