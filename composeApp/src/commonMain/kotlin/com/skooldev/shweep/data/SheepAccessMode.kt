package com.skooldev.shweep.data

import com.skooldev.shweep.purchase.EntitlementState

enum class SheepAccessMode {
    UNLIMITED,
    LIMITED,
    VERIFYING_PURCHASE
}

fun resolveSheepAccessMode(
    limitedSheepEnabled: Boolean,
    entitlement: EntitlementState
): SheepAccessMode {
    if (!limitedSheepEnabled) return SheepAccessMode.UNLIMITED

    return when (entitlement) {
        EntitlementState.PURCHASED -> SheepAccessMode.UNLIMITED
        EntitlementState.CHECKING -> SheepAccessMode.VERIFYING_PURCHASE
        EntitlementState.NOT_PURCHASED -> SheepAccessMode.LIMITED
        EntitlementState.UNAVAILABLE -> SheepAccessMode.LIMITED
    }
}
