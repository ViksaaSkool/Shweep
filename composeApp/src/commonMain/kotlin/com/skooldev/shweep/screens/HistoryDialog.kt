package com.skooldev.shweep.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.skooldev.shweep.data.Session
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryDialog(
    onDismiss: () -> Unit,
    sessionRepository: SessionRepository
) {
    val sessions by sessionRepository.sessions.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(Dimens.dialogWidthPercent)
                .fillMaxHeight(Dimens.dialogHeightPercent),
            shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.CardBackgroundHighAlpha
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.dialogPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.HISTORY_TITLE,
                        fontSize = Dimens.fontSizeXXLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = Strings.HISTORY_CLOSE,
                            fontSize = Dimens.fontSizeXLarge,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.NO_HISTORY,
                            fontSize = Dimens.fontSizeLarge,
                            color = AppColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        sessions.sortedByDescending { it.startTime }
                            .forEachIndexed { index, session ->
                                HistoryListItem(session = session)
                                if (index < sessions.size - 1) {
                                    Spacer(modifier = Modifier.height(Dimens.spacingLarge))
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryListItem(session: Session) {
    val dateTime = remember(session.startTime) {
        val instant = Instant.fromEpochMilliseconds(session.startTime)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val date = "${localDateTime.monthNumber}/${localDateTime.dayOfMonth}/${localDateTime.year}"
        val time = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"

        Pair(date, time)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.historyCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackgroundLowAlpha
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.historyItemPadding)
        ) {
            Text(
                text = dateTime.first,
                fontSize = Dimens.fontSizeSmall,
                color = AppColors.TextMuted
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No of sheep: ${session.sheepCount}",
                    fontSize = Dimens.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = dateTime.second,
                    fontSize = Dimens.fontSizeSmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}