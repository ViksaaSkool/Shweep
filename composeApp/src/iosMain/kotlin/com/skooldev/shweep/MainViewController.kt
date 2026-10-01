package com.skooldev.shweep

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.skooldev.shweep.purchase.RevenueCatConfig
import com.skooldev.shweep.purchase.RevenueCatPurchaseGateway
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController {
    val purchaseGateway = remember {
        val isDebug = Platform.isDebugBinary
        RevenueCatPurchaseGateway(RevenueCatConfig.iosSdkKey(isDebug))
    }
    App(
        purchaseGateway = purchaseGateway,
        visibilityMonitor = IosAppVisibilityMonitor()
    )
}
