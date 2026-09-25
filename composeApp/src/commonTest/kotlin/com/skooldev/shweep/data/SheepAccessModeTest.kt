package com.skooldev.shweep.data

import com.skooldev.shweep.purchase.EntitlementState
import kotlin.test.Test
import kotlin.test.assertEquals

class SheepAccessModeTest {

    @Test
    fun disabledLimitAlwaysGrantsUnlimited() {
        EntitlementState.entries.forEach { entitlement ->
            assertEquals(
                SheepAccessMode.UNLIMITED,
                resolveSheepAccessMode(limitedSheepEnabled = false, unlimitedSheepEntitlement = entitlement)
            )
        }
    }

    @Test
    fun purchasedEntitlementGrantsUnlimited() {
        assertEquals(
            SheepAccessMode.UNLIMITED,
            resolveSheepAccessMode(limitedSheepEnabled = true, unlimitedSheepEntitlement = EntitlementState.PURCHASED)
        )
    }

    @Test
    fun checkingEntitlementVerifiesOptimistically() {
        assertEquals(
            SheepAccessMode.VERIFYING_PURCHASE,
            resolveSheepAccessMode(limitedSheepEnabled = true, unlimitedSheepEntitlement = EntitlementState.CHECKING)
        )
    }

    @Test
    fun notPurchasedStaysLimited() {
        assertEquals(
            SheepAccessMode.LIMITED,
            resolveSheepAccessMode(limitedSheepEnabled = true, unlimitedSheepEntitlement = EntitlementState.NOT_PURCHASED)
        )
    }
}
