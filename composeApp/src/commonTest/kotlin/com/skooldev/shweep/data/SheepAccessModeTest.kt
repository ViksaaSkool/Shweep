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
                resolveSheepAccessMode(limitedSheepEnabled = false, entitlement = entitlement)
            )
        }
    }

    @Test
    fun purchasedEntitlementGrantsUnlimited() {
        assertEquals(
            SheepAccessMode.UNLIMITED,
            resolveSheepAccessMode(limitedSheepEnabled = true, entitlement = EntitlementState.PURCHASED)
        )
    }

    @Test
    fun checkingEntitlementVerifiesOptimistically() {
        assertEquals(
            SheepAccessMode.VERIFYING_PURCHASE,
            resolveSheepAccessMode(limitedSheepEnabled = true, entitlement = EntitlementState.CHECKING)
        )
    }

    @Test
    fun notPurchasedAndUnavailableStayLimited() {
        assertEquals(
            SheepAccessMode.LIMITED,
            resolveSheepAccessMode(limitedSheepEnabled = true, entitlement = EntitlementState.NOT_PURCHASED)
        )
        assertEquals(
            SheepAccessMode.LIMITED,
            resolveSheepAccessMode(limitedSheepEnabled = true, entitlement = EntitlementState.UNAVAILABLE)
        )
    }
}
