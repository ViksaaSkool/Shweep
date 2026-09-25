package com.skooldev.shweep

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.skooldev.shweep.purchase.RevenueCatConfig
import com.skooldev.shweep.purchase.RevenueCatPurchaseGateway

fun MainViewController() = ComposeUIViewController {
    val purchaseGateway = remember {
        RevenueCatPurchaseGateway(RevenueCatConfig.IOS_SDK_KEY)
    }
    App(
        purchaseGateway = purchaseGateway,
        visibilityMonitor = IosAppVisibilityMonitor()
    )
}
