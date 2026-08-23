package com.skooldev.shweep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch

enum class Screen {
    Start,
    Counting,
    Settings
}

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
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        when (currentScreen) {
            Screen.Start -> {
                StartScreen(
                    onGoToSleepClick = { currentScreen = Screen.Counting },
                    onHistoryClick = { showHistoryDialog = true },
                    onSettingsClick = { currentScreen = Screen.Settings }
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
                    onColorSelected = { color ->
                        scope.launch { settingsRepository.setSheepColor(color) }
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
    }
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        App()
    }
}
