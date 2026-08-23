package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val sheepColor: Flow<SheepColor>
    suspend fun setSheepColor(color: SheepColor)
}
