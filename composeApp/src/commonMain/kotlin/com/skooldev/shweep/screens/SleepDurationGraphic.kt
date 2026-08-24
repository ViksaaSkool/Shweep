package com.skooldev.shweep.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens

@Composable
internal fun SleepDurationGraphic(
    durationMinutes: Int?,
    modifier: Modifier = Modifier
) {
    val durationArcColor = AppColors.HistoryDurationArc
    val trackColor = AppColors.HistoryTrack
    val moonGlow = AppColors.MoonGlow
    val cardBackground = AppColors.CardBackgroundMediumAlpha
    val arcWidth = Dimens.historyDurationArcWidth

    Canvas(
        modifier = modifier
            .size(Dimens.historyMetricGraphicSize)
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w / 2f

        // Glow halo
        drawCircle(
            color = moonGlow.copy(alpha = 0.15f),
            radius = radius * 1.15f,
            center = center
        )

        // Track arc
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset.Zero,
            size = Size(w, h),
            style = Stroke(
                width = arcWidth.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Progress arc
        val fraction = if (durationMinutes != null) {
            (durationMinutes / 60f).coerceIn(0f, 1f)
        } else 0f

        if (fraction > 0f) {
            drawArc(
                color = durationArcColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = Stroke(
                    width = arcWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Crescent moon
        val moonRadius = radius * 0.32f

        drawCircle(
            color = moonGlow,
            radius = moonRadius,
            center = center
        )

        drawCircle(
            color = cardBackground,
            radius = moonRadius,
            center = center + Offset(
                x = moonRadius * 0.4f,
                y = -moonRadius * 0.1f
            )
        )

        // Stars
        val starColor = AppColors.TextPrimary.copy(alpha = 0.75f)
        val starRadius = 1.5.dp.toPx()
        val starOffset1 = Offset(center.x - radius * 0.45f, center.y - radius * 0.5f)
        val starOffset2 = Offset(center.x + radius * 0.35f, center.y - radius * 0.4f)
        val starOffset3 = Offset(center.x + radius * 0.55f, center.y + radius * 0.15f)

        drawCircle(color = starColor, radius = starRadius, center = starOffset1)
        drawCircle(color = starColor, radius = starRadius * 0.7f, center = starOffset2)
        drawCircle(color = starColor, radius = starRadius * 0.8f, center = starOffset3)

        // Invalid duration indicator
        if (durationMinutes == null) {
            drawCircle(
                color = trackColor,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
