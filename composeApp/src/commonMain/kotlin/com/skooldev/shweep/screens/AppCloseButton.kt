package com.skooldev.shweep.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens

/**
 * Standard close control: a rounded translucent surface with the same soft
 * lavender X used throughout the app.
 */
@Composable
internal fun AppCloseButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall))
            .semantics { this.contentDescription = contentDescription }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMeadowButtonBackground(colorFilter)
            drawCloseGlyph(colorFilter)
        }
    }
}

private fun DrawScope.drawCloseGlyph(colorFilter: ColorFilter?) {
    val w = size.width
    val h = size.height
    val margin = 0.34f

    drawLine(
        color = AppColors.HistoryDurationArc,
        start = Offset(w * margin, h * margin),
        end = Offset(w * (1f - margin), h * (1f - margin)),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter
    )
    drawLine(
        color = AppColors.HistoryDurationArc,
        start = Offset(w * (1f - margin), h * margin),
        end = Offset(w * margin, h * (1f - margin)),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter
    )
}

@Preview
@Composable
private fun AppCloseButtonPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF29233F))
            .padding(12.dp)
    ) {
        AppCloseButton(
            onClick = {},
            contentDescription = "Close"
        )
    }
}
