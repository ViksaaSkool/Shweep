package com.skooldev.shweep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.purchase.AndroidStorePurchaseGateway
import com.skooldev.shweep.purchase.MockStorePurchaseGateway

class MainActivity : ComponentActivity() {

    private lateinit var purchaseGateway: AndroidStorePurchaseGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        purchaseGateway = AndroidStorePurchaseGateway(this)

        setContent {
            App(purchaseGateway = purchaseGateway)
        }
    }

    override fun onResume() {
        super.onResume()
        if (
            FeatureFlags.LIMITED_DAILY_SHEEP_ENABLED &&
            ::purchaseGateway.isInitialized
        ) {
            purchaseGateway.refreshEntitlement()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(purchaseGateway = MockStorePurchaseGateway())
}
