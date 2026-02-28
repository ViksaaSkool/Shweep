package com.skooldev.shweep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.data.SessionRepositoryImpl
import com.skooldev.shweep.data.createDataStore
import com.skooldev.shweep.screens.StartScreen
import com.skooldev.shweep.screens.CountingSheepScreen
import com.skooldev.shweep.screens.HistoryDialog

enum class Screen {
    Start,
    Counting
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Start) }
        var showHistoryDialog by remember { mutableStateOf(false) }
        
        val sessionRepository = remember { SessionRepositoryImpl(createDataStore()) }
        
        when (currentScreen) {
            Screen.Start -> {
                StartScreen(
                    onGoToSleepClick = { currentScreen = Screen.Counting },
                    onHistoryClick = { showHistoryDialog = true }
                )
            }
            Screen.Counting -> {
                CountingSheepScreen(
                    onBackClick = { currentScreen = Screen.Start },
                    sessionRepository = sessionRepository
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