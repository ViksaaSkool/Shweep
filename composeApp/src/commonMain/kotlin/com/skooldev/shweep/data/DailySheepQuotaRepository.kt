package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface DailySheepQuotaRepository {
    val quota: Flow<DailySheepQuota>
    suspend fun refresh(): DailySheepQuota
    suspend fun tryConsumeSheep(): ConsumeSheepResult
}
