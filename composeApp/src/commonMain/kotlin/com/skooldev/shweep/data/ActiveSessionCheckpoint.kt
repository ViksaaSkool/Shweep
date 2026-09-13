package com.skooldev.shweep.data

import kotlinx.serialization.Serializable

@Serializable
data class ActiveSessionCheckpoint(
    val id: String,
    val startTime: Long,
    val lastInteractionTime: Long,
    val sheepCount: Int,
    val backgroundedAt: Long? = null
)
