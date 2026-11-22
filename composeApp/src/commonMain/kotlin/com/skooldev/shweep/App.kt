package com.skooldev.shweep

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenBackdrop = Color(0xFF1B1934)

enum class Screen {
    Start,
    Counting,
    History,
    Settings
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun App(
    purchaseGateway: StorePurchaseGateway,
    visibilityMonitor: AppVisibilityMonitor
) {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Start) }
        var animateNextScreenChange by remember { mutableStateOf(true) }
        var isTransitioning by remember { mutableStateOf(false) }

        fun navigateTo(target: Screen, animate: Boolean = true) {
            if (currentScreen == target) return
            animateNextScreenChange = animate
            isTransitioning = animate
            currentScreen = target
        }

        LaunchedEffect(currentScreen) {
            if (isTransitioning) {
                delay(SCREEN_TRANSITION_WINDOW_MILLIS)
                isTransitioning = false
            }
        }

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
                                    navigateTo(Screen.Start, animate = false)
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
        val showUpdateNotice = shouldShowUpdateNotice(seenUpdateNoticeVersion)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackdrop)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (animateNextScreenChange) {
                        screenContentTransform(
                            initial = initialState,
                            target = targetState,
                            style = platform.screenTransitionStyle,
                            reduceMotion = platform.prefersReducedMotion
                        )
                    } else {
                        instantScreenContentTransform()
                    }
                },
                label = "screen_transition",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                val isActive = screen == currentScreen

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { if (!isActive) invisibleToUser() }
                ) {
                    when (screen) {
                        Screen.Start -> {
                            StartScreen(
                                onGoToSleepClick = {
                                    if (currentScreen == Screen.Start) {
                                        coordinator.startSession()
                                        navigateTo(Screen.Counting)
                                    }
                                },
                                onHistoryClick = { navigateTo(Screen.History) },
                                onSettingsClick = { navigateTo(Screen.Settings) },
                                sheepColor = renderedColor
                            )
                        }
                        Screen.Counting -> {
                            CountingSheepScreen(
                                onBackClick = {
                                    coordinator.endSession(SessionEndReason.USER_EXIT)
                                    navigateTo(Screen.Start)
                                },
                                sessionRepository = sessionRepository,
                                freeSheepUsageRepository = freeSheepUsageRepository,
                                purchaseState = purchaseState,
                                onPurchase = {
                                    purchaseManager.purchase(PurchaseCatalog.UNLIMITED_SHEEP_PRODUCT)
                                },
                                onRestore = { entitlementId ->
                                    purchaseManager.restore(entitlementId)
                                },
                                onClearRestoreErrors = { purchaseManager.clearRestoreErrors() },
                                sheepArtwork = renderedColor.toArtwork(),
                                coordinator = coordinator,
                                visibilityMonitor = visibilityMonitor,
                                isActive = isActive
                            )
                        }
                        Screen.History -> {
                            HistoryScreen(
                                sessionRepository = sessionRepository,
                                onBack = { navigateTo(Screen.Start) }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                selectedColor = selectedColor,
                                hasColorfulSheep = purchaseState.hasColorfulSheep,
                                onSaveColor = { color ->
                                    scope.launch { settingsRepository.setSheepColor(color) }
                                    navigateTo(Screen.Start)
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
                                    shareText(
                                        text = AppLinks.inviteMessage,
                                        title = Strings.SHARE_SHWEEP
                                    )
                                },
                                onPurchase = { productId -> purchaseManager.purchase(productId) },
                                onRestorePurchases = { entitlementId ->
                                    purchaseManager.restore(entitlementId)
                                },
                                onClearRestoreErrors = { purchaseManager.clearRestoreErrors() },
                                versionLabel = versionLabel,
                                purchaseState = purchaseState,
                                onBack = { navigateTo(Screen.Start) }
                            )
                        }
                    }

                    if (!isActive) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent(PointerEventPass.Initial)
                                                .changes
                                                .forEach { it.consume() }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }

        BackHandler(
            enabled = currentScreen != Screen.Start || isTransitioning
        ) {
            if (currentScreen == Screen.Counting) {
                coordinator.endSession(SessionEndReason.USER_EXIT)
            }
            if (currentScreen != Screen.Start) {
                navigateTo(Screen.Start)
            }
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

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        App(
            purchaseGateway = MockStorePurchaseGateway(),
            visibilityMonitor = NoOpVisibilityMonitor
        )
    }
}
