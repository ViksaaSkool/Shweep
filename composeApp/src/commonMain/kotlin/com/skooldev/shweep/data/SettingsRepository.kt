package com.skooldev.shweep.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val sheepColor: Flow<SheepColor>
    val hasChosenSheepColor: Flow<Boolean>

    /** The app version whose "what's changed" notice the user has already dismissed, if any. */
    val seenUpdateNoticeVersion: Flow<String?>

    suspend fun setSheepColor(color: SheepColor)
    suspend fun markSheepColorChosen()
    suspend fun markUpdateNoticeSeen(version: String)
}
