package com.skooldev.shweep.data

import com.skooldev.shweep.purchase.EntitlementState
import kotlin.test.Test
import kotlin.test.assertEquals

class SheepAccessModeTest {

    @Test
    fun purchasedEntitlementGrantsUnlimited() {
        assertEquals(
            SheepAccessMode.UNLIMITED,
            resolveSheepAccessMode(EntitlementState.PURCHASED)
        )
    }

    @Test
    fun checkingEntitlementFallsBackToTheLocalAllowance() {
        assertEquals(
            SheepAccessMode.VERIFYING_PURCHASE,
            resolveSheepAccessMode(EntitlementState.CHECKING)
        )
    }

    @Test
    fun notPurchasedStaysLimited() {
        assertEquals(
            SheepAccessMode.LIMITED,
            resolveSheepAccessMode(EntitlementState.NOT_PURCHASED)
        )
    }
}
