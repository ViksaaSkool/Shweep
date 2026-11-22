package com.skooldev.shweep.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings
import kotlin.math.PI
import kotlin.math.sin

private val BadgeBackground = Color(0xFF151226)

/**
 * Moonlit music toggle for the counting meadow.
 *
 * The eighth note rests in cool lavender while paused. While playing it warms
 * to moonlight ivory, the badge switches to pause bars, and three tiny
 * fireflies drift around the note.
 */
@Composable
internal fun MeadowMusicButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val contentLabel = if (isPlaying) {
        Strings.CD_PAUSE_PIANO_MUSIC
    } else {
        Strings.CD_PLAY_PIANO_MUSIC
    }
    val stateLabel = if (isPlaying) {
        Strings.PIANO_MUSIC_PLAYING
    } else {
        Strings.PIANO_MUSIC_PAUSED
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall))
            .semantics {
                contentDescription = contentLabel
                stateDescription = stateLabel
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMeadowButtonBackground(colorFilter)
                drawNoteGlyph(isPlaying, colorFilter)
                drawPlaybackBadge(isPlaying, colorFilter)
            }

            if (isPlaying) {
                FireflyDots(
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = colorFilter
                )
            }
        }
    }
}

private fun DrawScope.drawNoteGlyph(
    isPlaying: Boolean,
    colorFilter: ColorFilter?
) {
    val noteColor = if (isPlaying) AppColors.MoonGlow else AppColors.HistoryDurationArc
    val w = size.width
    val h = size.height

    if (isPlaying) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(noteColor.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * 0.43f, h * 0.45f),
                radius = w * 0.34f
            ),
            radius = w * 0.34f,
            center = Offset(w * 0.43f, h * 0.45f),
            colorFilter = colorFilter
        )
    }

    val headCenter = Offset(w * 0.40f, h * 0.67f)
    rotate(degrees = -18f, pivot = headCenter) {
        drawOval(
            color = noteColor,
            topLeft = Offset(headCenter.x - w * 0.105f, headCenter.y - h * 0.073f),
            size = Size(w * 0.21f, h * 0.146f),
            colorFilter = colorFilter
        )
    }

    drawLine(
        color = noteColor,
        start = Offset(w * 0.495f, h * 0.655f),
        end = Offset(w * 0.495f, h * 0.29f),
        strokeWidth = w * 0.052f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter
    )

    val flag = Path().apply {
        moveTo(w * 0.505f, h * 0.29f)
        cubicTo(w * 0.66f, h * 0.32f, w * 0.68f, h * 0.41f, w * 0.58f, h * 0.48f)
        cubicTo(w * 0.63f, h * 0.40f, w * 0.58f, h * 0.36f, w * 0.505f, h * 0.37f)
        close()
    }
    drawPath(path = flag, color = noteColor, colorFilter = colorFilter)
}

private fun DrawScope.drawPlaybackBadge(
    isPlaying: Boolean,
    colorFilter: ColorFilter?
) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.735f, h * 0.735f)
    val radius = w * 0.155f
    val badgeColor = if (isPlaying) AppColors.MoonGlow else AppColors.HistoryDurationArc

    drawCircle(
        color = BadgeBackground,
        radius = radius,
        center = center,
        colorFilter = colorFilter
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = radius,
        center = center,
        style = Stroke(width = 0.75.dp.toPx()),
        colorFilter = colorFilter
    )

    if (isPlaying) {
        val halfHeight = radius * 0.50f
        val barWidth = radius * 0.30f
        val offset = radius * 0.22f
        drawLine(
            color = badgeColor,
            start = Offset(center.x - offset, center.y - halfHeight),
            end = Offset(center.x - offset, center.y + halfHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
            colorFilter = colorFilter
        )
        drawLine(
            color = badgeColor,
            start = Offset(center.x + offset, center.y - halfHeight),
            end = Offset(center.x + offset, center.y + halfHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
            colorFilter = colorFilter
        )
    } else {
        val triangle = Path().apply {
            moveTo(center.x - radius * 0.28f, center.y - radius * 0.42f)
            lineTo(center.x + radius * 0.48f, center.y)
            lineTo(center.x - radius * 0.28f, center.y + radius * 0.42f)
            close()
        }
        drawPath(path = triangle, color = badgeColor, colorFilter = colorFilter)
    }
}

@Composable
private fun FireflyDots(
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val transition = rememberInfiniteTransition(label = "piano_fireflies")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "piano_firefly_phase"
    ).value

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bases = listOf(
            Offset(w * 0.23f, h * 0.43f),
            Offset(w * 0.30f, h * 0.24f),
            Offset(w * 0.66f, h * 0.23f)
        )

        bases.forEachIndexed { index, base ->
            val angle = (phase + index / bases.size.toFloat()) * 2f * PI.toFloat()
            val bob = sin(angle) * h * 0.026f
            val alpha = 0.30f + 0.55f * ((sin(angle) + 1f) / 2f)
            drawCircle(
                color = AppColors.MoonGlow.copy(alpha = alpha),
                radius = w * 0.024f,
                center = base + Offset(0f, bob),
                colorFilter = colorFilter
            )
        }
    }
}

@Preview
@Composable
private fun MeadowMusicButtonPreview() {
    Row(
        modifier = Modifier
            .background(Color(0xFF29233F))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MeadowMusicButton(isPlaying = false, onClick = {})
        MeadowMusicButton(isPlaying = true, onClick = {})
    }
}
