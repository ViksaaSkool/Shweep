package com.skooldev.shweep.data

import kotlinx.serialization.Serializable

@Serializable
enum class SessionEndReason {
    USER_EXIT,
    BACKGROUND_TIMEOUT,
    RECOVERED_AFTER_TERMINATION,
    UNKNOWN
}

@Serializable
data class Session(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val sheepCount: Int,
    val endReason: SessionEndReason = SessionEndReason.UNKNOWN
)
