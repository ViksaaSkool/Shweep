package com.skooldev.shweep.data

import com.skooldev.shweep.purchase.EntitlementState

enum class SheepAccessMode {
    UNLIMITED,
    LIMITED,
    VERIFYING_PURCHASE
}

/**
 * Whether the local free allowance applies, based solely on the `unlimited_sheep` entitlement.
 * Colorful sheep never affect the allowance.
 *
 * While the entitlement is still being checked, counting falls back to the local allowance rather
 * than granting unverified paid access.
 */
fun resolveSheepAccessMode(unlimitedSheepEntitlement: EntitlementState): SheepAccessMode =
    when (unlimitedSheepEntitlement) {
        EntitlementState.PURCHASED -> SheepAccessMode.UNLIMITED
        EntitlementState.CHECKING -> SheepAccessMode.VERIFYING_PURCHASE
        EntitlementState.NOT_PURCHASED -> SheepAccessMode.LIMITED
    }
