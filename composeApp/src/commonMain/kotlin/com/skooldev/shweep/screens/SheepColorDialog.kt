package com.skooldev.shweep.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.black_sheep
import shweep.composeapp.generated.resources.sheep
import com.skooldev.shweep.data.SheepColor
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.Strings

@Composable
fun SheepColorDialog(
    selectedColor: SheepColor,
    onConfirm: (SheepColor) -> Unit
) {
    var selection by remember { mutableStateOf<SheepColor?>(null) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(Dimens.dialogWidthPercent),
            shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.CardBackgroundHighAlpha
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.cardPaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.CHOOSE_SHEEP_TITLE,
                    fontSize = Dimens.fontSizeXXLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

                SheepColorOption(
                    label = Strings.SHEEP_COLOR_WHITE,
                    imageRes = Res.drawable.sheep,
                    selected = selection == SheepColor.WHITE,
                    onClick = { selection = SheepColor.WHITE }
                )

                Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                SheepColorOption(
                    label = Strings.SHEEP_COLOR_BLACK,
                    imageRes = Res.drawable.black_sheep,
                    selected = selection == SheepColor.BLACK,
                    onClick = { selection = SheepColor.BLACK }
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

                Button(
                    onClick = { selection?.let(onConfirm) },
                    enabled = selection != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.buttonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(
                        text = Strings.CHOOSE_SHEEP_CONTINUE,
                        fontSize = Dimens.fontSizeLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SheepColorDialogPreview() {
    SheepColorDialog(
        selectedColor = SheepColor.WHITE,
        onConfirm = {}
    )
}
