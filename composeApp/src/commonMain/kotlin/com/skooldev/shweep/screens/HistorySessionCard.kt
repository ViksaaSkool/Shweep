package com.skooldev.shweep.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings

@Composable
internal fun HistorySessionCard(
    session: HistorySessionUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.historyCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackgroundMediumAlpha
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimens.historySessionPadding)
        ) {
            // Header: date + start time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = session.dateText,
                    fontSize = Dimens.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = Strings.HISTORY_STARTED_AT.replace("%s", session.startTimeText),
                    fontSize = Dimens.fontSizeSmall,
                    color = AppColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            HorizontalDivider(
                color = AppColors.HistoryDivider
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

            // Two metrics side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Time to sleep metric
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Dimens.spacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SleepDurationGraphic(
                        durationMinutes = session.durationMinutes
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                    Text(
                        text = session.durationText,
                        fontSize = Dimens.fontSizeXLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                    Text(
                        text = Strings.HISTORY_TIME_TO_SLEEP,
                        fontSize = Dimens.fontSizeSmall,
                        color = AppColors.TextMuted
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(Dimens.historyMetricGraphicSize)
                        .padding(horizontal = Dimens.spacingSmall),
                    color = AppColors.HistoryDivider
                )

                // Sheep count metric
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Dimens.spacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MiniSheepFlock(
                        iconCount = session.displayedSheepIcons
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                    Text(
                        text = session.sheepCount.toString(),
                        fontSize = Dimens.fontSizeXLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                    Text(
                        text = Strings.HISTORY_SHEEP_COUNTED,
                        fontSize = Dimens.fontSizeSmall,
                        color = AppColors.TextMuted
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun HistorySessionCardPreview() {
    HistorySessionCard(
        session = HistorySessionUiModel(
            id = "1",
            dateText = "Monday, 1/15/2024",
            startTimeText = "22:42",
            durationText = "16 min",
            durationMinutes = 16,
            sheepCount = 42,
            displayedSheepIcons = 4,
            hasOverflowingFlock = false
        )
    )
}
