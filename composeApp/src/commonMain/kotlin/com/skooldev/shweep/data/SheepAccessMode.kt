package com.skooldev.shweep.data

import com.skooldev.shweep.purchase.EntitlementState

enum class SheepAccessMode {
    UNLIMITED,
    LIMITED,
    VERIFYING_PURCHASE
}

/**
 * Whether the 35-sheep allowance applies, based solely on the `unlimited_sheep` entitlement.
 * Colorful sheep never affect the allowance.
 */
fun resolveSheepAccessMode(
    limitedSheepEnabled: Boolean,
    unlimitedSheepEntitlement: EntitlementState
): SheepAccessMode {
    if (!limitedSheepEnabled) return SheepAccessMode.UNLIMITED

    return when (unlimitedSheepEntitlement) {
        EntitlementState.PURCHASED -> SheepAccessMode.UNLIMITED
        EntitlementState.CHECKING -> SheepAccessMode.VERIFYING_PURCHASE
        EntitlementState.NOT_PURCHASED -> SheepAccessMode.LIMITED
    }
}
