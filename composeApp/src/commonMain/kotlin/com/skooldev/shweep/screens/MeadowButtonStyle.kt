package com.skooldev.shweep.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens

private const val BUTTON_BACKGROUND_ALPHA = 0.60f

internal val MeadowButtonBackground =
    AppColors.CardBackgroundMediumAlpha.copy(alpha = BUTTON_BACKGROUND_ALPHA)

/**
 * Rounded translucent surface matching the settings gear button. Shared by
 * every control in the app so their backgrounds stay consistent.
 *
 * [colorFilter] lets the meadow controls dim along with the landscape.
 */
internal fun DrawScope.drawMeadowButtonBackground(colorFilter: ColorFilter? = null) {
    drawRoundRect(
        color = MeadowButtonBackground,
        cornerRadius = CornerRadius(Dimens.cardCornerRadiusSmall.toPx()),
        colorFilter = colorFilter
    )
}
