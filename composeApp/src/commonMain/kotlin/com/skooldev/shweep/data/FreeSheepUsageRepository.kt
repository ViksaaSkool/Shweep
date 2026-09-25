package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

/**
 * Local persistence for the free sheep allowance. There is no backend counterpart: this state is
 * stored on the device and is never sent anywhere.
 */
interface FreeSheepUsageRepository {
    val usage: Flow<FreeSheepUsage>
    suspend fun refresh(): FreeSheepUsage
    suspend fun tryConsumeSheep(): ConsumeSheepResult
}
