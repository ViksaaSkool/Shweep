package com.skooldev.shweep

import androidx.compose.ui.window.ComposeUIViewController
import com.skooldev.shweep.purchase.IosStorePurchaseGateway

fun MainViewController() = ComposeUIViewController {
    val gateway = IosStorePurchaseGateway()
    App(
        purchaseGateway = gateway,
        visibilityMonitor = IosAppVisibilityMonitor()
    )
}
