package com.skooldev.shweep.data

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val sheepCount: Int
)