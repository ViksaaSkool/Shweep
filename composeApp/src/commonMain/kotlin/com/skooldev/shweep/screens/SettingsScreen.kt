package com.skooldev.shweep.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_start
import shweep.composeapp.generated.resources.black_sheep
import shweep.composeapp.generated.resources.sheep
import com.skooldev.shweep.data.SheepColor
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import com.skooldev.shweep.purchase.EntitlementState
import com.skooldev.shweep.purchase.PurchaseOperation
import com.skooldev.shweep.purchase.UnlimitedSheepPurchaseState

@Composable
fun SettingsScreen(
    selectedColor: SheepColor,
    onSaveColor: (SheepColor) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onInviteFriendsClick: () -> Unit,
    onBuyCoffeeClick: () -> Unit,
    onBuyUnlimited: () -> Unit,
    onRestorePurchases: () -> Unit,
    limitedSheepEnabled: Boolean,
    purchaseState: UnlimitedSheepPurchaseState,
    onBack: () -> Unit
) {
    var pendingColor by remember { mutableStateOf(selectedColor) }
    val hasChanges = pendingColor != selectedColor

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.background_start),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(Dimens.screenPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.titlePillHeight)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = Strings.SETTINGS_CLOSE,
                        tint = AppColors.TextPrimary
                    )
                }

                Surface(
                    modifier = Modifier
                        .width(Dimens.titlePillWidth)
                        .height(Dimens.titlePillHeight)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(Dimens.titlePillCornerRadius),
                    color = AppColors.TitlePillAlpha
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = Strings.SETTINGS_TITLE,
                            fontSize = Dimens.fontSizeXXXLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.CardBackgroundMediumAlpha
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPaddingLarge)) {
                    Text(
                        text = Strings.SHEEP_COLOR_TITLE,
                        fontSize = Dimens.fontSizeMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                    SheepColorOption(
                        label = Strings.SHEEP_COLOR_WHITE,
                        imageRes = Res.drawable.sheep,
                        selected = pendingColor == SheepColor.WHITE,
                        onClick = { pendingColor = SheepColor.WHITE }
                    )

                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                    SheepColorOption(
                        label = Strings.SHEEP_COLOR_BLACK,
                        imageRes = Res.drawable.black_sheep,
                        selected = pendingColor == SheepColor.BLACK,
                        onClick = { pendingColor = SheepColor.BLACK }
                    )
                }
            }

            if (hasChanges) {
                Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                Button(
                    onClick = { onSaveColor(pendingColor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(
                        text = Strings.SAVE,
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.CardBackgroundMediumAlpha
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPaddingLarge)) {
                    LinkRow(
                        label = Strings.PRIVACY_POLICY,
                        subtitle = Strings.LINK_OPENS_IN_BROWSER,
                        onClick = onPrivacyPolicyClick
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimens.spacingMedium),
                        color = AppColors.TextPrimary.copy(alpha = 0.2f)
                    )

                    LinkRow(
                        label = Strings.TERMS_OF_SERVICE,
                        subtitle = Strings.LINK_OPENS_IN_BROWSER,
                        onClick = onTermsOfServiceClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

            if (limitedSheepEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.CardBackgroundMediumAlpha
                    )
                ) {
                    Column(modifier = Modifier.padding(Dimens.cardPaddingLarge)) {
                        Text(
                            text = Strings.UNLIMITED_SHEEP_TITLE,
                            fontSize = Dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                        if (purchaseState.isPurchased) {
                            Text(
                                text = Strings.UNLIMITED_SHEEP_ACTIVE,
                                fontSize = Dimens.fontSizeLarge,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextPrimary
                            )
                        } else {
                            Text(
                                text = Strings.UNLIMITED_SHEEP_DESCRIPTION_DETAIL,
                                fontSize = Dimens.fontSizeSmall,
                                color = AppColors.TextMuted,
                                lineHeight = Dimens.lineHeightMedium
                            )

                            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

                            val buyText = when {
                                purchaseState.operation == PurchaseOperation.PURCHASING -> Strings.UNLIMITED_SHEEP_PURCHASING
                                purchaseState.operation == PurchaseOperation.RESTORING -> Strings.UNLIMITED_SHEEP_RESTORING
                                purchaseState.entitlement == EntitlementState.CHECKING -> Strings.UNLIMITED_SHEEP_PURCHASE_LOADING
                                purchaseState.entitlement == EntitlementState.UNAVAILABLE -> Strings.UNLIMITED_SHEEP_UNAVAILABLE
                                purchaseState.isProductLoaded -> "${Strings.UNLIMITED_SHEEP_PURCHASE_TITLE} · ${purchaseState.product!!.localizedPrice}"
                                else -> Strings.UNLIMITED_SHEEP_PURCHASE_LOADING
                            }

                            Button(
                                onClick = onBuyUnlimited,
                                enabled = purchaseState.canBuy && purchaseState.operation == PurchaseOperation.IDLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.buttonHeight),
                                shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Primary
                                )
                            ) {
                                Text(
                                    text = buyText,
                                    fontSize = Dimens.fontSizeLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                            OutlinedButton(
                                onClick = onRestorePurchases,
                                enabled = purchaseState.operation == PurchaseOperation.IDLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.buttonHeight),
                                shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AppColors.ButtonBackgroundAlpha
                                )
                            ) {
                                Text(
                                    text = "Restore purchases",
                                    fontSize = Dimens.fontSizeLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
            }

            Button(
                onClick = onInviteFriendsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text(
                    text = Strings.INVITE_FRIENDS,
                    fontSize = Dimens.fontSizeLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

            Button(
                onClick = onBuyCoffeeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text(
                    text = Strings.BUY_DEVELOPER_COFFEE,
                    fontSize = Dimens.fontSizeLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = Dimens.fontSizeLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = Dimens.fontSizeSmall,
                color = AppColors.TextMuted
            )
        }

        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.TextPrimary.copy(alpha = 0.7f)
        )
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        selectedColor = SheepColor.WHITE,
        onSaveColor = {},
        onPrivacyPolicyClick = {},
        onTermsOfServiceClick = {},
        onInviteFriendsClick = {},
        onBuyCoffeeClick = {},
        onBuyUnlimited = {},
        onRestorePurchases = {},
        limitedSheepEnabled = false,
        purchaseState = UnlimitedSheepPurchaseState(),
        onBack = {}
    )
}
