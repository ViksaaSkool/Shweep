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
    val scale: Float = 1f,
    val artwork: SheepArtwork = SheepArtwork.WHITE,
    val gaitPhaseRadians: Float = 0f
)
