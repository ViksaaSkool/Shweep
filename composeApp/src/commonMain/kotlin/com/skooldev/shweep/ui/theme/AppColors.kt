package com.skooldev.shweep.ui.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // Primary Colors
    val Primary = Color(0xFF7B6B9E)
    val PrimaryDark = Color(0xFF4A3B69)
    val PrimaryLight = Color(0xFF6B5B95)
    val PrimaryVariant = Color(0xFF5A4A79)
    
    // Background Colors
    val CardBackground = Color(0xFF4A3B69)
    val CardBackgroundSecondary = Color(0xFF5A4A79)
    val TitlePillBackground = Color(0xFF6B5B95)
    
    // Text Colors
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.8f)
    val TextMuted = Color.White.copy(alpha = 0.7f)
    
    // Alpha Variants
    val CardBackgroundHighAlpha = Color(0xFF4A3B69).copy(alpha = 0.95f)
    val CardBackgroundMediumAlpha = Color(0xFF4A3B69).copy(alpha = 0.7f)
    val CardBackgroundLowAlpha = Color(0xFF4A3B69).copy(alpha = 0.6f)
    val ButtonBackgroundAlpha = Color(0xFF5A4A79).copy(alpha = 0.5f)
    val TitlePillAlpha = Color(0xFF6B5B95).copy(alpha = 0.6f)
}
