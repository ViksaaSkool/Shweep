package com.skooldev.shweep

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skooldev.shweep.purchase.MockStorePurchaseGateway
import com.skooldev.shweep.purchase.RevenueCatConfig
import com.skooldev.shweep.purchase.RevenueCatPurchaseGateway
import com.skooldev.shweep.purchase.StorePurchaseGateway

class MainActivity : ComponentActivity() {

    private lateinit var purchaseGateway: StorePurchaseGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        purchaseGateway = RevenueCatPurchaseGateway(
            RevenueCatConfig.androidSdkKey(isDebug)
        )

        setContent {
            App(
                purchaseGateway = purchaseGateway,
                visibilityMonitor = AndroidAppVisibilityMonitor()
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        purchaseGateway = MockStorePurchaseGateway(),
        visibilityMonitor = AndroidAppVisibilityMonitor()
    )
}
