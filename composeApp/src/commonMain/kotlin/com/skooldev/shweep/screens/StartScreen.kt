package com.skooldev.shweep.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_start
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings

@Composable
fun StartScreen(
    onGoToSleepClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image with sheep and landscape
        Image(
            painter = painterResource(Res.drawable.background_start),
            contentDescription = Strings.CD_BACKGROUND_IMAGE,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXXXXLarge))

            // Title Pill
            Surface(
                modifier = Modifier
                    .width(Dimens.titlePillWidth)
                    .height(Dimens.titlePillHeight),
                shape = RoundedCornerShape(Dimens.titlePillCornerRadius),
                color = AppColors.TitlePillAlpha
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.APP_NAME,
                        fontSize = Dimens.fontSizeXXXLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

            // Tagline
            Text(
                text = Strings.START_TAGLINE,
                fontSize = Dimens.fontSizeMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = Dimens.lineHeightMedium
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))

            // Button Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.horizontalPadding),
                shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.CardBackgroundMediumAlpha
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.cardPaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingXLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Go to sleep button
                    Button(
                        onClick = onGoToSleepClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.buttonHeight),
                        shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        Text(
                            text = Strings.BUTTON_GO_TO_SLEEP,
                            fontSize = Dimens.fontSizeLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // History button
                    OutlinedButton(
                        onClick = onHistoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.buttonHeight),
                        shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppColors.ButtonBackgroundAlpha
                        )
                    ) {
                        Text(
                            text = Strings.BUTTON_HISTORY,
                            fontSize = Dimens.fontSizeLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun StartScreenPreview() {
    StartScreen(
        onGoToSleepClick = {},
        onHistoryClick = {}
    )
}
