package com.skooldev.shweep.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens

internal data class SheepIconData(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)

/**
 * A small flock of [iconCount] sheep, where each sheep stands for
 * [SHEEP_PER_ICON] counted sheep. When the count exceeds what the flock can
 * show, [overflowCount] appears as a "+n" badge so a long session is never
 * silently indistinguishable from a short one.
 */
@Composable
internal fun MiniSheepFlock(
    iconCount: Int,
    overflowCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val woolColor = Color(0xFFF5E8DD)
    val shadowColor = Color(0xFFB9A8D4)
    val faceColor = Color(0xFFD9A47F)
    val outlineColor = Color(0xFF3B3154)

    val positions = rememberSheepIconPositions(iconCount)

    Box(
        modifier = modifier
            .size(Dimens.historyFlockWidth, Dimens.historyFlockHeight),
        contentAlignment = Alignment.TopEnd
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            for (icon in positions) {
                drawSheepIcon(
                    centerX = icon.x,
                    centerY = icon.y,
                    size = icon.size,
                    alpha = icon.alpha,
                    woolColor = woolColor,
                    shadowColor = shadowColor,
                    faceColor = faceColor,
                    outlineColor = outlineColor
                )
            }
        }

        if (overflowCount > 0) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMuted,
                modifier = Modifier
                    .offset(x = 4.dp, y = 2.dp)
                    .background(
                        color = AppColors.CardBackgroundMediumAlpha,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun rememberSheepIconPositions(count: Int): List<SheepIconData> {
    return remember(count) {
        when (count) {
            0 -> emptyList()
            1 -> listOf(
                SheepIconData(56f, 32f, 30f, 1f)
            )
            2 -> listOf(
                SheepIconData(28f, 30f, 28f, 1f),
                SheepIconData(72f, 30f, 28f, 1f)
            )
            3 -> listOf(
                SheepIconData(20f, 34f, 28f, 1f),
                SheepIconData(56f, 30f, 30f, 1f),
                SheepIconData(88f, 34f, 28f, 1f)
            )
            4 -> listOf(
                SheepIconData(30f, 34f, 28f, 1f),
                SheepIconData(68f, 34f, 28f, 1f),
                SheepIconData(48f, 14f, 24f, 0.8f),
                SheepIconData(80f, 14f, 24f, 0.8f)
            )
            else -> listOf(
                SheepIconData(20f, 40f, 28f, 1f),
                SheepIconData(54f, 40f, 28f, 1f),
                SheepIconData(88f, 40f, 28f, 1f),
                SheepIconData(36f, 14f, 24f, 0.8f),
                SheepIconData(64f, 14f, 24f, 0.8f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSheepIcon(
    centerX: Float,
    centerY: Float,
    size: Float,
    alpha: Float,
    woolColor: Color,
    shadowColor: Color,
    faceColor: Color,
    outlineColor: Color
) {
    val halfSize = size / 2f
    val bodyRadius = halfSize * 0.65f
    val headRadius = halfSize * 0.45f

    // Body shadow
    drawCircle(
        color = shadowColor.copy(alpha = alpha),
        radius = bodyRadius,
        center = Offset(centerX, centerY + 1f)
    )

    // Body wool
    drawCircle(
        color = woolColor.copy(alpha = alpha),
        radius = bodyRadius,
        center = Offset(centerX, centerY)
    )

    // Head
    drawCircle(
        color = faceColor.copy(alpha = alpha),
        radius = headRadius,
        center = Offset(centerX - bodyRadius * 0.3f, centerY - bodyRadius * 0.2f)
    )

    // Outline
    drawCircle(
        color = outlineColor.copy(alpha = alpha * 0.6f),
        radius = bodyRadius,
        center = Offset(centerX, centerY),
        style = Stroke(1.dp.toPx())
    )
}
