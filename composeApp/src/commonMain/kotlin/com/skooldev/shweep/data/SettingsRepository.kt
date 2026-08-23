package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val sheepColor: Flow<SheepColor>
    val hasChosenSheepColor: Flow<Boolean>
    suspend fun setSheepColor(color: SheepColor)
    suspend fun markSheepColorChosen()
}
