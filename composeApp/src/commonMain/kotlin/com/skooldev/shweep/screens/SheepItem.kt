package com.skooldev.shweep.screens

data class SheepItem(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var scale: Float = 1f
)