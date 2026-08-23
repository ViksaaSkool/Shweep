package com.skooldev.shweep.screens

enum class SheepArtwork {
    WHITE,
    BLACK
}

data class SheepItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val artwork: SheepArtwork = SheepArtwork.WHITE,
    val gaitPhaseRadians: Float = 0f,
    val isDriftingAway: Boolean = false,
    val driftSpeedX: Float = 0f,
    val ageSeconds: Float = 0f,
    val lifetimeSeconds: Float = 12.5f,
    val zigzagDirection: Float = 1f,
    val nextZigzagTurnIn: Float = 0.9f,
    val zigzagTurnInterval: Float = 0.9f
)
