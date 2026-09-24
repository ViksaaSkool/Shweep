package com.skooldev.shweep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.data.DailySheepQuotaRepositoryImpl
import com.skooldev.shweep.data.SessionEndReason
import com.skooldev.shweep.data.SessionRepositoryImpl
import com.skooldev.shweep.data.SettingsRepositoryImpl
import com.skooldev.shweep.data.SheepColor
import com.skooldev.shweep.data.createDataStore
import com.skooldev.shweep.data.toArtwork
import com.skooldev.shweep.purchase.DisabledStorePurchaseGateway
import com.skooldev.shweep.purchase.MockStorePurchaseGateway
import com.skooldev.shweep.purchase.StorePurchaseGateway
import com.skooldev.shweep.purchase.UnlimitedSheepPurchaseManager
import com.skooldev.shweep.screens.CountingSheepScreen
import com.skooldev.shweep.screens.HistoryScreen
import com.skooldev.shweep.screens.SettingsScreen
import com.skooldev.shweep.screens.SheepColorDialog
import com.skooldev.shweep.screens.StartScreen
import com.skooldev.shweep.screens.UpdateNoticeDialog
import com.skooldev.shweep.ui.theme.Strings
import kotlinx.coroutines.launch

enum class Screen {
    Start,
    Counting,
    History,
    Settings
}

/**
 * Sentinel for the update-notice preference while DataStore has not emitted yet, so the notice
 * never flashes for a user who has already dismissed it.
 */
private const val UPDATE_NOTICE_NOT_LOADED = "\u0000not-loaded"

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun App(
    purchaseGateway: StorePurchaseGateway,
    visibilityMonitor: AppVisibilityMonitor
) {
    MaterialTheme {
        val limitedSheepEnabled = FeatureFlags.LIMITED_DAILY_SHEEP_ENABLED
        var currentScreen by remember { mutableStateOf(Screen.Start) }

        val dataStore = remember { createDataStore() }
        val sessionRepository = remember(dataStore) { SessionRepositoryImpl(dataStore) }
        val settingsRepository = remember(dataStore) { SettingsRepositoryImpl(dataStore) }
        val dailySheepQuotaRepository = remember(dataStore) { DailySheepQuotaRepositoryImpl(dataStore) }

        val effectiveGateway = remember(purchaseGateway, limitedSheepEnabled) {
            if (limitedSheepEnabled) purchaseGateway else DisabledStorePurchaseGateway()
        }
        val purchaseManager = remember(effectiveGateway) {
            UnlimitedSheepPurchaseManager(effectiveGateway)
        }
        val purchaseState by purchaseManager.state.collectAsState()

        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        val platform = remember { getPlatform() }
        val versionLabel = "${platform.appVersion} (${platform.appBuild})"

        val coordinator = remember(sessionRepository, scope) {
            CountingSessionCoordinator(sessionRepository, scope)
        }

        LaunchedEffect(Unit) {
            coordinator.recoverOrphanedSession()
        }

        DisposableEffect(purchaseManager, limitedSheepEnabled) {
            if (limitedSheepEnabled) {
                purchaseManager.start()
            }
            onDispose {
                if (limitedSheepEnabled) {
                    purchaseManager.stop()
                }
            }
        }

        LaunchedEffect(visibilityMonitor, currentScreen) {
            visibilityMonitor.events.collect { event ->
                if (currentScreen == Screen.Counting) {
                    when (event) {
                        AppVisibilityEvent.Background -> {
                            coordinator.onBackground()
                        }
                        AppVisibilityEvent.Foreground -> {
                            when (coordinator.onForeground()) {
                                CountingSessionCoordinator.ForegroundResult.SessionEnded -> {
                                    currentScreen = Screen.Start
                                }
                                CountingSessionCoordinator.ForegroundResult.Resumed -> {
                                    // Session continues
                                }
                                CountingSessionCoordinator.ForegroundResult.NoSession -> {
                                    // No active session
                                }
                            }
                        }
                    }
                }
            }
        }

        val selectedColor by settingsRepository.sheepColor.collectAsState(
            initial = SheepColor.WHITE
        )
        val hasChosenSheepColor by settingsRepository.hasChosenSheepColor.collectAsState(
            initial = true
        )
        val seenUpdateNoticeVersion by settingsRepository.seenUpdateNoticeVersion.collectAsState(
            initial = UPDATE_NOTICE_NOT_LOADED
        )
        val showUpdateNotice = limitedSheepEnabled &&
            seenUpdateNoticeVersion != UPDATE_NOTICE_NOT_LOADED &&
            seenUpdateNoticeVersion != FeatureFlags.UPDATE_NOTICE_VERSION

        when (currentScreen) {
            Screen.Start -> {
                StartScreen(
                    onGoToSleepClick = {
                        coordinator.startSession()
                        currentScreen = Screen.Counting
                    },
                    onHistoryClick = { currentScreen = Screen.History },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    sheepColor = selectedColor
                )
            }
            Screen.Counting -> {
                CountingSheepScreen(
                    onBackClick = {
                        coordinator.endSession(SessionEndReason.USER_EXIT)
                        currentScreen = Screen.Start
                    },
                    sessionRepository = sessionRepository,
                    dailySheepQuotaRepository = dailySheepQuotaRepository,
                    limitedSheepEnabled = limitedSheepEnabled,
                    purchaseState = purchaseState,
                    onPurchase = { purchaseManager.purchase() },
                    onRestore = { purchaseManager.restore() },
                    sheepArtwork = selectedColor.toArtwork(),
                    coordinator = coordinator
                )
            }
            Screen.History -> {
                HistoryScreen(
                    sessionRepository = sessionRepository,
                    onBack = { currentScreen = Screen.Start }
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
                    onContactSupportClick = {
                        uriHandler.openUri(AppLinks.CONTACT_SUPPORT)
                    },
                    onInviteFriendsClick = {
                        shareText(text = AppLinks.inviteMessage, title = Strings.SHARE_SHWEEP)
                    },
                    onBuyUnlimited = { purchaseManager.purchase() },
                    onRestorePurchases = { purchaseManager.restore() },
                    versionLabel = versionLabel,
                    limitedSheepEnabled = limitedSheepEnabled,
                    purchaseState = purchaseState,
                    onBack = { currentScreen = Screen.Start }
                )
            }
        }

        BackHandler(enabled = currentScreen == Screen.Settings || currentScreen == Screen.Counting || currentScreen == Screen.History) {
            if (currentScreen == Screen.Counting) {
                coordinator.endSession(SessionEndReason.USER_EXIT)
            }
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

        if (showUpdateNotice) {
            UpdateNoticeDialog(
                onContinue = {
                    scope.launch {
                        settingsRepository.markUpdateNoticeSeen(FeatureFlags.UPDATE_NOTICE_VERSION)
                    }
                },
                onPrivacyPolicy = { uriHandler.openUri(AppLinks.PRIVACY_POLICY) },
                onTermsOfService = { uriHandler.openUri(AppLinks.TERMS_OF_SERVICE) }
            )
        }
    }
}

private class NoOpVisibilityMonitor : AppVisibilityMonitor {
    override val events = kotlinx.coroutines.flow.emptyFlow<AppVisibilityEvent>()
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        App(
            purchaseGateway = MockStorePurchaseGateway(),
            visibilityMonitor = NoOpVisibilityMonitor()
        )
    }
}
