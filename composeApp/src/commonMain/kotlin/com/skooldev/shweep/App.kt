package com.skooldev.shweep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.data.SessionRepositoryImpl
import com.skooldev.shweep.data.SettingsRepositoryImpl
import com.skooldev.shweep.data.SheepColor
import com.skooldev.shweep.data.createDataStore
import com.skooldev.shweep.data.toArtwork
import com.skooldev.shweep.screens.StartScreen
import com.skooldev.shweep.screens.CountingSheepScreen
import com.skooldev.shweep.screens.HistoryDialog
import com.skooldev.shweep.screens.SettingsScreen
import com.skooldev.shweep.screens.SheepColorDialog
import kotlinx.coroutines.launch

enum class Screen {
    Start,
    Counting,
    Settings
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Start) }
        var showHistoryDialog by remember { mutableStateOf(false) }

        val dataStore = remember { createDataStore() }
        val sessionRepository = remember(dataStore) { SessionRepositoryImpl(dataStore) }
        val settingsRepository = remember(dataStore) { SettingsRepositoryImpl(dataStore) }

        val selectedColor by settingsRepository.sheepColor.collectAsState(
            initial = SheepColor.WHITE
        )
        val hasChosenSheepColor by settingsRepository.hasChosenSheepColor.collectAsState(
            initial = true
        )
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        when (currentScreen) {
            Screen.Start -> {
                StartScreen(
                    onGoToSleepClick = { currentScreen = Screen.Counting },
                    onHistoryClick = { showHistoryDialog = true },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    sheepColor = selectedColor
                )
            }
            Screen.Counting -> {
                CountingSheepScreen(
                    onBackClick = { currentScreen = Screen.Start },
                    sessionRepository = sessionRepository,
                    sheepArtwork = selectedColor.toArtwork()
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    selectedColor = selectedColor,
                    onSaveColor = { color ->
                        scope.launch { settingsRepository.setSheepColor(color) }
                        currentScreen = Screen.Start
                    },
                    onPrivacyPolicyClick = {
                        uriHandler.openUri(AppLinks.PRIVACY_POLICY)
                    },
                    onTermsOfServiceClick = {
                        uriHandler.openUri(AppLinks.TERMS_OF_SERVICE)
                    },
                    onBuyCoffeeClick = {
                        uriHandler.openUri(AppLinks.BUY_ME_A_COFFEE)
                    },
                    onBack = { currentScreen = Screen.Start }
                )
            }
        }

        if (showHistoryDialog) {
            HistoryDialog(
                onDismiss = { showHistoryDialog = false },
                sessionRepository = sessionRepository
            )
        }

        BackHandler(enabled = currentScreen == Screen.Settings) {
            currentScreen = Screen.Start
        }

        if (!hasChosenSheepColor) {
            SheepColorDialog(
                selectedColor = selectedColor,
                onConfirm = { color ->
                    scope.launch {
                        settingsRepository.setSheepColor(color)
                        settingsRepository.markSheepColorChosen()
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        App()
    }
}
