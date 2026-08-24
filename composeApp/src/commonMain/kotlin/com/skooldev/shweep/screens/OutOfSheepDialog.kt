package com.skooldev.shweep.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings

@OptIn(ExperimentalTime::class)
@Composable
fun OutOfSheepDialog(
    nextResetEpochMillis: Long,
    onDismiss: () -> Unit,
    onResetReached: () -> Unit
) {
    var remainingMillis by remember {
        mutableLongStateOf(
            (nextResetEpochMillis - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0)
        )
    }

    LaunchedEffect(nextResetEpochMillis) {
        while (remainingMillis > 0) {
            remainingMillis = (nextResetEpochMillis - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0)
            if (remainingMillis <= 0L) {
                onResetReached()
                break
            }
            delay(1000L)
        }
    }

    val hours = remainingMillis / 3_600_000
    val minutes = (remainingMillis % 3_600_000) / 60_000
    val seconds = (remainingMillis % 60_000) / 1000
    val timerText = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(Dimens.dialogWidthPercent),
            shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.CardBackgroundHighAlpha
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.dialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.OUT_OF_SHEEP_TITLE,
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

                Text(
                    text = Strings.OUT_OF_SHEEP_MESSAGE,
                    fontSize = Dimens.fontSizeMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = Dimens.lineHeightMedium
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXXLarge))

                Surface(
                    shape = RoundedCornerShape(Dimens.cardCornerRadiusSmall),
                    color = AppColors.CardBackgroundMediumAlpha
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.cardPaddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timerText,
                            fontSize = Dimens.fontSizeXXXLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                        Text(
                            text = Strings.OUT_OF_SHEEP_TIMER_LABEL,
                            fontSize = Dimens.fontSizeSmall,
                            color = AppColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXXLarge))

                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary,
                        disabledContainerColor = AppColors.Primary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = Strings.UNLIMITED_SHEEP_COMING_SOON,
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.ButtonBackgroundAlpha
                    )
                ) {
                    Text(
                        text = Strings.WAIT_UNTIL_RESET,
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun OutOfSheepDialogPreview() {
    OutOfSheepDialog(
        nextResetEpochMillis = Clock.System.now().toEpochMilliseconds() + 5 * 3_600_000,
        onDismiss = {},
        onResetReached = {}
    )
}
