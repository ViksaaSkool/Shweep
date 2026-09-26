package com.skooldev.shweep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.data.DataStoreFreeSheepUsageRepository
import com.skooldev.shweep.data.SessionEndReason
import com.skooldev.shweep.data.SessionRepositoryImpl
import com.skooldev.shweep.data.SettingsRepositoryImpl
import com.skooldev.shweep.data.SheepColor
import com.skooldev.shweep.data.createDataStore
import com.skooldev.shweep.data.effectiveSheepColor
import com.skooldev.shweep.data.toArtwork
import com.skooldev.shweep.purchase.MockStorePurchaseGateway
import com.skooldev.shweep.purchase.PurchaseCatalog
import com.skooldev.shweep.purchase.PurchaseManager
import com.skooldev.shweep.purchase.StorePurchaseGateway
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
        var currentScreen by remember { mutableStateOf(Screen.Start) }

        val dataStore = remember { createDataStore() }
        val sessionRepository = remember(dataStore) { SessionRepositoryImpl(dataStore) }
        val settingsRepository = remember(dataStore) { SettingsRepositoryImpl(dataStore) }
        val freeSheepUsageRepository = remember(dataStore) { DataStoreFreeSheepUsageRepository(dataStore) }

        val purchaseManager = remember(purchaseGateway) {
            PurchaseManager(purchaseGateway)
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

        DisposableEffect(purchaseManager) {
            purchaseManager.start()
            onDispose {
                purchaseManager.stop()
            }
        }

        LaunchedEffect(visibilityMonitor, currentScreen) {
            visibilityMonitor.events.collect { event ->
                when (event) {
                    AppVisibilityEvent.Background -> {
                        if (currentScreen == Screen.Counting) {
                            coordinator.onBackground()
                        }
                    }
                    AppVisibilityEvent.Foreground -> {
                        purchaseManager.refresh()
                        if (currentScreen == Screen.Counting) {
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
        val renderedColor = effectiveSheepColor(selectedColor, purchaseState.hasColorfulSheep)
        val hasChosenSheepColor by settingsRepository.hasChosenSheepColor.collectAsState(
            initial = true
        )
        val seenUpdateNoticeVersion by settingsRepository.seenUpdateNoticeVersion.collectAsState(
            initial = UPDATE_NOTICE_NOT_LOADED
        )
        val showUpdateNotice = seenUpdateNoticeVersion != UPDATE_NOTICE_NOT_LOADED &&
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
                    sheepColor = renderedColor
                )
            }
            Screen.Counting -> {
                CountingSheepScreen(
                    onBackClick = {
                        coordinator.endSession(SessionEndReason.USER_EXIT)
                        currentScreen = Screen.Start
                    },
                    sessionRepository = sessionRepository,
                    freeSheepUsageRepository = freeSheepUsageRepository,
                    purchaseState = purchaseState,
                    onPurchase = { purchaseManager.purchase(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT) },
                    onRestore = { entitlementId -> purchaseManager.restore(entitlementId) },
                    onClearRestoreErrors = { purchaseManager.clearRestoreErrors() },
                    sheepArtwork = renderedColor.toArtwork(),
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
                    hasColorfulSheep = purchaseState.hasColorfulSheep,
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
                    onPurchase = { productId -> purchaseManager.purchase(productId) },
                    onRestorePurchases = { entitlementId -> purchaseManager.restore(entitlementId) },
                    onClearRestoreErrors = { purchaseManager.clearRestoreErrors() },
                    versionLabel = versionLabel,
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
                hasColorfulSheep = purchaseState.hasColorfulSheep,
                onConfirm = { color ->
                    scope.launch {
                        settingsRepository.setSheepColor(color)
                        settingsRepository.markSheepColorChosen()
                    }
                },
                onPurchaseColorful = {
                    purchaseManager.purchase(PurchaseCatalog.COLORFUL_SHEEP_PRODUCT)
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
