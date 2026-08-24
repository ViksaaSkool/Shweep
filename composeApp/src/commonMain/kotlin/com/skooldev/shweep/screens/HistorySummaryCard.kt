package com.skooldev.shweep.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings

@Composable
internal fun HistorySummaryCard(
    summary: HistorySummaryUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackgroundHighAlpha
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.historySummaryPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SleepDurationGraphic(
                durationMinutes = summary.averageDurationMinutes
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

            Text(
                text = Strings.HISTORY_SUMMARY_TITLE,
                fontSize = Dimens.fontSizeMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))

            Text(
                text = summary.averageDurationText,
                fontSize = Dimens.fontSizeXXXLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${summary.totalNights} nights",
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${summary.averageSheepCount} avg sheep",
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            Text(
                text = Strings.HISTORY_ESTIMATE_NOTE,
                fontSize = Dimens.fontSizeSmall,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun HistorySummaryCardPreview() {
    HistorySummaryCard(
        summary = HistorySummaryUiModel(
            totalNights = 12,
            averageDurationText = "18 min",
            averageDurationMinutes = 18,
            averageSheepCount = 31
        )
    )
}
