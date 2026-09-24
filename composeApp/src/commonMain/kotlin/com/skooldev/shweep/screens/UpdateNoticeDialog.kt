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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings

/**
 * One-time informational notice shown after the user updates to a release that introduces the
 * sheep allowance and the optional purchase. It is informational only: it does not request consent
 * and it is not a purchase prompt.
 */
@Composable
fun UpdateNoticeDialog(
    onContinue: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(Dimens.dialogWidthPercent),
            shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.CardBackgroundHighAlpha
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.dialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.UPDATE_NOTICE_TITLE,
                    fontSize = Dimens.fontSizeXXLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

                Text(
                    text = Strings.UPDATE_NOTICE_MESSAGE,
                    fontSize = Dimens.fontSizeMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = Dimens.lineHeightMedium
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXXLarge))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(
                        text = Strings.UPDATE_NOTICE_CONTINUE,
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onPrivacyPolicy) {
                        Text(
                            text = Strings.PRIVACY_POLICY,
                            fontSize = Dimens.fontSizeSmall,
                            color = AppColors.TextMuted
                        )
                    }

                    TextButton(onClick = onTermsOfService) {
                        Text(
                            text = Strings.TERMS_OF_SERVICE,
                            fontSize = Dimens.fontSizeSmall,
                            color = AppColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun UpdateNoticeDialogPreview() {
    UpdateNoticeDialog(
        onContinue = {},
        onPrivacyPolicy = {},
        onTermsOfService = {}
    )
}
