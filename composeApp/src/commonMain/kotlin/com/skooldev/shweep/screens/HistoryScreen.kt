package com.skooldev.shweep.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import com.skooldev.shweep.data.MockSessionRepository
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings
import kotlinx.datetime.TimeZone

@Composable
fun HistoryScreen(
    sessionRepository: SessionRepository,
    onBack: () -> Unit
) {
    val sessions by sessionRepository.sessions.collectAsState(initial = emptyList())
    val timeZone = remember { TimeZone.currentSystemDefault() }

    val sessionUiModels = remember(sessions, timeZone) {
        sessions.toHistoryUiModels(timeZone)
    }

    val summaryUiModel = remember(sessions) {
        sessions.toHistorySummaryUiModel()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(Res.drawable.background_counting),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Darkening gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x991B1934),
                            Color(0xCC29233F)
                        )
                    )
                )
        )

        // Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(Dimens.screenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = Strings.SETTINGS_CLOSE,
                        tint = AppColors.TextPrimary
                    )
                }

                Surface(
                    modifier = Modifier
                        .width(Dimens.titlePillWidth)
                        .height(Dimens.titlePillHeight),
                    shape = RoundedCornerShape(Dimens.titlePillCornerRadius),
                    color = AppColors.TitlePillAlpha
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = Strings.HISTORY_TITLE,
                            fontSize = Dimens.fontSizeXXXLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sessionUiModels.isEmpty()) {
                    item {
                        EmptyHistoryContent()
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                        HistorySummaryCard(
                            summary = summaryUiModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimens.spacingXXLarge))

                        Text(
                            text = Strings.HISTORY_RECENT_NIGHTS,
                            fontSize = Dimens.fontSizeLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
                    }

                    items(
                        items = sessionUiModels,
                        key = { it.id }
                    ) { session ->
                        HistorySessionCard(
                            session = session,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Dimens.historyCardSpacing))
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.spacingXXXXLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SleepDurationGraphic(
                durationMinutes = null
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXXLarge))

            Text(
                text = Strings.HISTORY_EMPTY_TITLE,
                fontSize = Dimens.fontSizeLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            Text(
                text = Strings.HISTORY_EMPTY_MESSAGE,
                fontSize = Dimens.fontSizeMedium,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = Dimens.lineHeightMedium
            )
        }
    }
}

@Preview
@Composable
fun HistoryScreenWithDataPreview() {
    HistoryScreen(
        sessionRepository = MockSessionRepository(),
        onBack = {}
    )
}
