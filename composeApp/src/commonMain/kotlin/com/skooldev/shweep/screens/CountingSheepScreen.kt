package com.skooldev.shweep.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.AppVisibilityEvent
import com.skooldev.shweep.AppVisibilityMonitor
import com.skooldev.shweep.CountingSessionCoordinator
import com.skooldev.shweep.MonotonicClock
import com.skooldev.shweep.NoOpVisibilityMonitor
import com.skooldev.shweep.audio.createMeadowMusicPlayer
import com.skooldev.shweep.audio.createSheepBleatPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import com.skooldev.shweep.data.ConsumeSheepResult
import com.skooldev.shweep.data.FreeSheepUsage
import com.skooldev.shweep.data.FreeSheepUsageRepository
import com.skooldev.shweep.data.MockFreeSheepUsageRepository
import com.skooldev.shweep.data.MockSessionRepository
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.data.SheepAccessMode
import com.skooldev.shweep.data.resolveSheepAccessMode
import com.skooldev.shweep.purchase.PurchaseCatalog
import com.skooldev.shweep.purchase.PurchaseState
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlin.math.floor
import kotlin.random.Random

private const val MIN_UPWARD_DISTANCE_DP = 48f
private const val DRAG_ACTIVATION_DISTANCE_DP = 16f
private const val DRAG_ACTIVATION_DURATION_MILLIS = 150L
private const val SLOW_GESTURE_THRESHOLD_DP_PER_SECOND = 700f

private enum class SheepGestureMode {
    PENDING,
    DRAGGING
}

@Composable
fun CountingSheepScreen(
    onBackClick: () -> Unit,
    sessionRepository: SessionRepository,
    freeSheepUsageRepository: FreeSheepUsageRepository,
    purchaseState: PurchaseState,
    onPurchase: () -> Unit,
    onRestore: (entitlementId: String) -> Unit,
    onClearRestoreErrors: () -> Unit,
    sheepArtwork: SheepArtwork = SheepArtwork.WHITE,
    coordinator: CountingSessionCoordinator,
    visibilityMonitor: AppVisibilityMonitor = NoOpVisibilityMonitor,
    isActive: Boolean = true
) {
    val unlimitedSheepEntitlement =
        purchaseState.entitlementState(PurchaseCatalog.UNLIMITED_SHEEP_ENTITLEMENT)
    val accessMode = remember(unlimitedSheepEntitlement) {
        resolveSheepAccessMode(unlimitedSheepEntitlement)
    }
    var sheepCount by remember { mutableStateOf(0) }
    var screenSize by remember { mutableStateOf(Size.Zero) }
    var frameCounter by remember { mutableIntStateOf(0) }
    val sheepList = remember { mutableStateOf<List<SheepItem>>(emptyList()) }
    val density = LocalDensity.current
    val densityScale = density.density
    val scope = rememberCoroutineScope()

    var draggedSheep by remember { mutableStateOf<SheepItem?>(null) }
    var frozenSheepId by remember { mutableStateOf<Int?>(null) }
    var exhaustedUsage by remember { mutableStateOf<FreeSheepUsage?>(null) }

    val bleatPlayer = remember { createSheepBleatPlayer() }
    val musicPlayer = remember { createMeadowMusicPlayer() }
    val isMusicPlaying by musicPlayer.isPlaying.collectAsState()
    var isAppInForeground by remember { mutableStateOf(true) }
    var isCheckingMusicAccess by remember { mutableStateOf(false) }
    var musicAccessCheckJob by remember { mutableStateOf<Job?>(null) }
    val currentIsActive by rememberUpdatedState(isActive)

    val toggleMusic: () -> Unit = {
        if (currentIsActive) {
            if (isMusicPlaying) {
                // Stopping the piano is always allowed, even at zero sheep.
                musicPlayer.pause()
            } else if (accessMode == SheepAccessMode.UNLIMITED) {
                musicPlayer.play()
            } else if (!isCheckingMusicAccess) {
                isCheckingMusicAccess = true
                musicAccessCheckJob = scope.launch {
                    try {
                        val usage = runCatching { freeSheepUsageRepository.refresh() }.getOrNull()
                        when {
                            usage == null -> Unit
                            !currentIsActive -> Unit
                            usage.isExhausted -> exhaustedUsage = usage
                            isAppInForeground -> musicPlayer.play()
                        }
                    } finally {
                        isCheckingMusicAccess = false
                        musicAccessCheckJob = null
                    }
                }
            }
        }
    }

    val grayness by coordinator.grayness.collectAsState()

    val sheepBaseSize = 80.dp
    val sheepBaseSizePx = with(density) { sheepBaseSize.toPx() }
    val minUpwardDistancePx = with(density) { MIN_UPWARD_DISTANCE_DP.dp.toPx() }
    val dragActivationDistancePx = with(density) { DRAG_ACTIVATION_DISTANCE_DP.dp.toPx() }
    val slowGestureThresholdPxPerSecond = with(density) { SLOW_GESTURE_THRESHOLD_DP_PER_SECOND.dp.toPx() }

    val currentScreenWidth = screenSize.width
    val currentScreenHeight = screenSize.height
    val currentPlayAreaStartY = currentScreenHeight * 0.35f
    val currentPlayAreaHeight = currentScreenHeight - currentPlayAreaStartY
    val meadowCapacity = calculateMeadowCapacity(
        screenWidth = currentScreenWidth,
        playAreaHeight = currentPlayAreaHeight,
        sheepBaseSizePx = sheepBaseSizePx
    )

    // grayness: 0f = colorful, 1f = grayscale. Compose saturation uses the
    // opposite convention, so invert here at the rendering boundary.
    val sceneColorMatrix = remember(grayness) {
        ColorMatrix().apply { setToSaturation(1f - grayness) }
    }
    val sceneColorFilter = remember(sceneColorMatrix) {
        ColorFilter.colorMatrix(sceneColorMatrix)
    }

    LaunchedEffect(bleatPlayer) {
        runCatching { Res.readBytes("files/mee_sheep.mp3") }
            .onSuccess { bleatPlayer.prepare(it) }
    }

    LaunchedEffect(musicPlayer) {
        runCatching { Res.readBytes("files/piano_sheep.mp3") }
            .onSuccess { musicPlayer.prepare(it) }
    }

    DisposableEffect(bleatPlayer) {
        onDispose { bleatPlayer.release() }
    }

    DisposableEffect(musicPlayer) {
        onDispose { musicPlayer.release() }
    }

    LaunchedEffect(visibilityMonitor, bleatPlayer, musicPlayer) {
        visibilityMonitor.events.collect { event ->
            when (event) {
                AppVisibilityEvent.Background -> {
                    isAppInForeground = false
                    bleatPlayer.stop()
                    musicPlayer.pause()
                }
                AppVisibilityEvent.Foreground -> {
                    isAppInForeground = true
                }
            }
        }
    }

    LaunchedEffect(isActive, bleatPlayer, musicPlayer) {
        if (!isActive) {
            musicAccessCheckJob?.cancel()
            musicAccessCheckJob = null
            isCheckingMusicAccess = false
            bleatPlayer.stop()
            musicPlayer.pause()
        }
    }

    fun createSheep(
        sample: GestureSample,
        x: Float,
        y: Float,
        screenWidth: Float,
        sheepBaseSizePx: Float,
        elapsedMillis: Long
    ) {
        scope.launch {
            if (accessMode == SheepAccessMode.UNLIMITED) {
                val lifetime = randomLifetimeSeconds()
                val turnInterval = randomTurnIntervalSeconds()
                val newSheep = SheepItem(
                    id = sheepCount,
                    x = x.coerceIn(0f, screenWidth - sheepBaseSizePx),
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 500f,
                    vy = (Random.nextFloat() - 0.5f) * 500f,
                    artwork = sheepArtwork,
                    motionState = SheepMotionState.ACTIVE,
                    ageSeconds = 0f,
                    lifetimeSeconds = lifetime,
                    nextZigzagTurnIn = turnInterval,
                    zigzagTurnInterval = turnInterval,
                    gaitPhaseRadians = initialGaitPhase(sheepCount)
                )
                val updatedSheep = makeRoomForNewSheep(sheepList.value, meadowCapacity, screenWidth, sheepBaseSizePx)
                sheepList.value = updatedSheep + newSheep
                sheepCount++
                coordinator.recordSuccess(sample, elapsedMillis)
                coordinator.incrementSheep()
            } else {
                when (val result = freeSheepUsageRepository.tryConsumeSheep()) {
                    is ConsumeSheepResult.Allowed -> {
                        val lifetime = randomLifetimeSeconds()
                        val turnInterval = randomTurnIntervalSeconds()
                        val newSheep = SheepItem(
                            id = sheepCount,
                            x = x.coerceIn(0f, screenWidth - sheepBaseSizePx),
                            y = y,
                            vx = (Random.nextFloat() - 0.5f) * 500f,
                            vy = (Random.nextFloat() - 0.5f) * 500f,
                            artwork = sheepArtwork,
                            motionState = SheepMotionState.ACTIVE,
                            ageSeconds = 0f,
                            lifetimeSeconds = lifetime,
                            nextZigzagTurnIn = turnInterval,
                            zigzagTurnInterval = turnInterval,
                            gaitPhaseRadians = initialGaitPhase(sheepCount)
                        )
                        val updatedSheep = makeRoomForNewSheep(sheepList.value, meadowCapacity, screenWidth, sheepBaseSizePx)
                        sheepList.value = updatedSheep + newSheep
                        sheepCount++
                        coordinator.recordSuccess(sample, elapsedMillis)
                        coordinator.incrementSheep()

                        if (result.usage.isExhausted) {
                            exhaustedUsage = result.usage
                        }
                    }
                    is ConsumeSheepResult.Exhausted -> {
                        coordinator.recordAttempt(sample, elapsedMillis)
                        exhaustedUsage = result.usage
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val rawDeltaSeconds =
                        ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0).toFloat()
                    val physicsDeltaSeconds =
                        rawDeltaSeconds.coerceAtMost(SheepSimulation.MAX_DELTA_SECONDS)
                    lastFrameTimeNanos = frameTimeNanos
                    frameCounter++

                    coordinator.tick(MonotonicClock.nowMillis(), rawDeltaSeconds)

                    val screenWidth = screenSize.width
                    val screenHeight = screenSize.height

                    if (screenWidth > 0 && screenHeight > 0 && physicsDeltaSeconds > 0f) {
                        val playAreaStartY = screenHeight * 0.35f

                        val stepped = SheepSimulation.step(
                            sheepList = sheepList.value,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            playAreaStartY = playAreaStartY,
                            sheepBaseSizePx = sheepBaseSizePx,
                            deltaSeconds = physicsDeltaSeconds,
                            frozenSheepId = frozenSheepId
                        )

                        sheepList.value = stepped.filter { shouldKeepSheep(it, screenWidth, screenHeight, playAreaStartY, sheepBaseSizePx) }
                    }
                } else {
                    lastFrameTimeNanos = frameTimeNanos
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> screenSize = size.toSize() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Header buttons consume their own down events, so taps and
                    // drags that start on the close or music controls never
                    // reach the meadow.
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val downPosition = down.position
                    val downTimeMillis = down.uptimeMillis

                    draggedSheep = null
                    coordinator.onPointerActivity(MonotonicClock.nowMillis())

                    val targetSheepId = findSheepIdAt(
                        sheepList = sheepList.value,
                        x = downPosition.x,
                        y = downPosition.y,
                        sheepBaseSizePx = sheepBaseSizePx
                    )

                    var pendingLongPress = targetSheepId != null
                    var bleatTriggered = false
                    var dragStarted = false
                    var dragStartedAtMillis = downTimeMillis
                    var upTimeMillis = downTimeMillis
                    var lastEventTimeMillis = downTimeMillis
                    var totalDragY = 0f
                    var gestureMode = SheepGestureMode.PENDING
                    var endedWithUp = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(downTimeMillis, downPosition)

                    try {
                        while (true) {
                            val remainingLongPressMillis =
                                viewConfiguration.longPressTimeoutMillis -
                                    (lastEventTimeMillis - downTimeMillis)

                            val event = if (pendingLongPress) {
                                withTimeoutOrNull(remainingLongPressMillis.coerceAtLeast(0L)) {
                                    awaitPointerEvent()
                                }
                            } else {
                                awaitPointerEvent()
                            }

                            if (event == null) {
                                val targetStillPresent = targetSheepId != null &&
                                    sheepList.value.any { it.id == targetSheepId }

                                if (pendingLongPress && targetStillPresent && isAppInForeground) {
                                    bleatPlayer.play()
                                    bleatTriggered = true
                                    frozenSheepId = targetSheepId
                                }
                                pendingLongPress = false
                                continue
                            }

                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            lastEventTimeMillis = change.uptimeMillis

                            if (event.changes.count { it.pressed } > 1) {
                                pendingLongPress = false
                            }

                            val distanceFromDown = (change.position - downPosition).getDistance()

                            if (bleatTriggered) {
                                change.consume()
                                if (change.changedToUpIgnoreConsumed()) {
                                    upTimeMillis = change.uptimeMillis
                                    endedWithUp = true
                                    break
                                }
                                continue
                            }

                            if (
                                isSheepLongPressTriggered(
                                    hasTarget = targetSheepId != null,
                                    distancePx = distanceFromDown,
                                    touchSlopPx = viewConfiguration.touchSlop,
                                    elapsedMillis = change.uptimeMillis - downTimeMillis,
                                    timeoutMillis = viewConfiguration.longPressTimeoutMillis
                                )
                            ) {
                                val targetStillPresent = targetSheepId != null &&
                                    sheepList.value.any { it.id == targetSheepId }

                                if (targetStillPresent && isAppInForeground) {
                                    bleatPlayer.play()
                                    bleatTriggered = true
                                    frozenSheepId = targetSheepId
                                    pendingLongPress = false
                                    change.consume()
                                    if (change.changedToUpIgnoreConsumed()) {
                                        upTimeMillis = change.uptimeMillis
                                        endedWithUp = true
                                        break
                                    }
                                    continue
                                }
                                pendingLongPress = false
                            }

                            if (pendingLongPress && distanceFromDown > viewConfiguration.touchSlop) {
                                pendingLongPress = false
                            }

                            if (!dragStarted && distanceFromDown > viewConfiguration.touchSlop) {
                                dragStarted = true
                                dragStartedAtMillis = change.uptimeMillis
                                totalDragY = change.position.y - downPosition.y
                                velocityTracker.resetTracking()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                            } else if (dragStarted) {
                                totalDragY = change.position.y - downPosition.y
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                            }

                            if (dragStarted) {
                                change.consume()
                                coordinator.onPointerActivity(MonotonicClock.nowMillis())

                                val currentUpwardDistance =
                                    maxOf(0f, downPosition.y - change.position.y)

                                if (gestureMode == SheepGestureMode.PENDING) {
                                    val elapsedMillis = change.uptimeMillis - dragStartedAtMillis
                                    if (totalDragY < 0f && currentUpwardDistance >= dragActivationDistancePx) {
                                        val gestureDurationSeconds =
                                            elapsedMillis.coerceAtLeast(1) / 1000f
                                        val averageUpwardSpeed =
                                            currentUpwardDistance / gestureDurationSeconds

                                        if (
                                            elapsedMillis >= DRAG_ACTIVATION_DURATION_MILLIS &&
                                            averageUpwardSpeed < slowGestureThresholdPxPerSecond
                                        ) {
                                            gestureMode = SheepGestureMode.DRAGGING
                                        }
                                    }
                                }

                                if (gestureMode == SheepGestureMode.DRAGGING) {
                                    draggedSheep = createDraggedSheepPreview(
                                        position = change.position,
                                        screenWidth = screenSize.width,
                                        screenHeight = screenSize.height,
                                        sheepBaseSizePx = sheepBaseSizePx,
                                        artwork = sheepArtwork,
                                        id = sheepCount
                                    )
                                }
                            }

                            if (change.changedToUpIgnoreConsumed()) {
                                upTimeMillis = change.uptimeMillis
                                endedWithUp = true
                                break
                            }
                        }
                    } finally {
                        if (!endedWithUp) {
                            draggedSheep = null
                            gestureMode = SheepGestureMode.PENDING
                        }
                        frozenSheepId = null
                    }

                    if (endedWithUp && !bleatTriggered && dragStarted) {
                        val screenWidth = screenSize.width
                        val screenHeight = screenSize.height
                        val playAreaStartY = screenHeight * 0.35f
                        val playAreaHeight = screenHeight - playAreaStartY
                        val now = MonotonicClock.nowMillis()
                        val elapsed = upTimeMillis - dragStartedAtMillis

                        if (screenWidth > 0 && playAreaHeight > 0) {
                            val currentDraggedSheep = draggedSheep
                            if (gestureMode == SheepGestureMode.DRAGGING &&
                                currentDraggedSheep != null
                            ) {
                                val dropX = currentDraggedSheep.x.coerceIn(
                                    0f,
                                    maxOf(0f, screenWidth - sheepBaseSizePx)
                                )
                                val dropY = currentDraggedSheep.y.coerceIn(
                                    playAreaStartY,
                                    maxOf(playAreaStartY, screenHeight - sheepBaseSizePx)
                                )
                                val distance = maxOf(0f, -totalDragY)
                                val speed =
                                    if (elapsed > 0) distance / (elapsed / 1000f) else 0f

                                val sample = GestureSample(
                                    mode = GestureMode.DRAG,
                                    durationMillis = elapsed,
                                    upwardDistanceDp = distance / densityScale,
                                    effectiveSpeedDpPerSecond = speed / densityScale,
                                    completed = true,
                                    cancelled = false
                                )

                                createSheep(
                                    sample,
                                    dropX,
                                    dropY,
                                    screenWidth,
                                    sheepBaseSizePx,
                                    now
                                )
                            } else {
                                val isUpwardSwipe = totalDragY < -minUpwardDistancePx
                                if (isUpwardSwipe) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val releaseUpwardSpeed = -velocity.y
                                    val averageUpwardSpeed = maxOf(
                                        0f,
                                        -totalDragY / maxOf(0.001f, elapsed / 1000f)
                                    )
                                    val effectiveUpwardSpeed =
                                        maxOf(averageUpwardSpeed, releaseUpwardSpeed)

                                    if (effectiveUpwardSpeed >= slowGestureThresholdPxPerSecond) {
                                        val distanceDp = -totalDragY / densityScale
                                        val speedDp = effectiveUpwardSpeed / densityScale

                                        val sample = GestureSample(
                                            mode = GestureMode.FLICK,
                                            durationMillis = elapsed,
                                            upwardDistanceDp = distanceDp,
                                            effectiveSpeedDpPerSecond = speedDp,
                                            completed = true,
                                            cancelled = false
                                        )

                                        val newX = Random.nextFloat() *
                                            maxOf(0f, screenWidth - sheepBaseSizePx)
                                        val newY = playAreaStartY + Random.nextFloat() *
                                            maxOf(0f, playAreaHeight - sheepBaseSizePx)
                                        createSheep(
                                            sample,
                                            newX,
                                            newY,
                                            screenWidth,
                                            sheepBaseSizePx,
                                            now
                                        )
                                    } else {
                                        coordinator.recordAttempt(
                                            GestureSample(
                                                mode = GestureMode.FLICK,
                                                durationMillis = elapsed,
                                                upwardDistanceDp = -totalDragY / densityScale,
                                                effectiveSpeedDpPerSecond =
                                                    effectiveUpwardSpeed / densityScale,
                                                completed = false,
                                                cancelled = false
                                            ),
                                            now
                                        )
                                    }
                                }
                            }
                        }
                    }

                    draggedSheep = null
                    gestureMode = SheepGestureMode.PENDING
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.background_counting),
                contentDescription = Strings.CD_COUNTING_BACKGROUND,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = sceneColorFilter
            )

            LayeredSheepCanvas(
                sheepList = sheepList.value,
                draggedSheep = draggedSheep,
                sheepBaseSizePx = sheepBaseSizePx,
                frameCounter = frameCounter,
                modifier = Modifier.fillMaxSize(),
                colorFilter = sceneColorFilter
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppCloseButton(
                    onClick = onBackClick,
                    contentDescription = Strings.CD_CLOSE_COUNTING,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                    colorFilter = sceneColorFilter
                )

                MeadowMusicButton(
                    isPlaying = isMusicPlaying,
                    onClick = toggleMusic,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                    colorFilter = sceneColorFilter
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (sheepCount == 0) {
                SwipeUpIndicator()
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
        }
    }

    LaunchedEffect(accessMode) {
        if (accessMode == SheepAccessMode.UNLIMITED) {
            exhaustedUsage = null
        }
    }

    LaunchedEffect(accessMode) {
        if (accessMode != SheepAccessMode.UNLIMITED) {
            val usage = freeSheepUsageRepository.refresh()
            if (currentIsActive && usage.isExhausted) {
                exhaustedUsage = usage
            }
        }
    }

    if (isActive) {
        exhaustedUsage?.let { usage ->
            if (accessMode != SheepAccessMode.UNLIMITED) {
                OutOfSheepDialog(
                    cooldownEndsAtEpochMillis = usage.cooldownEndsAtEpochMillis,
                    purchaseState = purchaseState,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                    onDismiss = { exhaustedUsage = null },
                    onResetReached = {
                        exhaustedUsage = null
                        scope.launch { freeSheepUsageRepository.refresh() }
                    },
                    onClearRestoreErrors = onClearRestoreErrors
                )
            }
        }
    }
}

private fun makeRoomForNewSheep(
    sheep: List<SheepItem>,
    meadowCapacity: Int,
    screenWidth: Float,
    sheepBaseSizePx: Float
): List<SheepItem> {
    val activeSheep = sheep.filter { it.motionState == SheepMotionState.ACTIVE }
    val isFull = activeSheep.size >= meadowCapacity
    if (!isFull || activeSheep.isEmpty()) return sheep

    val sorted = activeSheep.sortedBy { it.id }
    val candidates = sorted.take(2)
    val updatedSheep = mutableListOf<SheepItem>()
    candidates.forEach { candidate ->
        updatedSheep.add(startDrifting(candidate, screenWidth, sheepBaseSizePx))
    }
    updatedSheep.addAll(sheep.filter { it !in candidates })
    return updatedSheep
}

private fun randomLifetimeSeconds(): Float = Random.nextFloat() * 5f + 10f
private fun randomTurnIntervalSeconds(): Float = Random.nextFloat() * 0.5f + 0.7f
private fun initialGaitPhase(id: Int): Float = SheepGait.positiveModulo(id * 2.3999632f, SheepGait.TAU)

@Composable
private fun SwipeUpIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_animation")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_animation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .offset(y = offsetY.dp),
            tint = Color.White.copy(alpha = alpha)
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

        Text(
            text = Strings.SWIPE_UP,
            fontSize = Dimens.fontSizeMedium,
            color = Color.White.copy(alpha = alpha),
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
fun CountingSheepScreenPreview() {
    CountingSheepScreen(
        onBackClick = {},
        sessionRepository = MockSessionRepository(),
        freeSheepUsageRepository = MockFreeSheepUsageRepository(),
        purchaseState = PurchaseState(),
        onPurchase = {},
        onRestore = { _ -> },
        onClearRestoreErrors = {},
        coordinator = CountingSessionCoordinator(
            sessionRepository = MockSessionRepository(),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        )
    )
}

internal fun calculateMeadowCapacity(
    screenWidth: Float,
    playAreaHeight: Float,
    sheepBaseSizePx: Float
): Int {
    if (screenWidth <= 0f || playAreaHeight <= 0f || sheepBaseSizePx <= 0f) return 1
    val columns = floor(screenWidth / sheepBaseSizePx).toInt().coerceAtLeast(1)
    val rows = floor(playAreaHeight / sheepBaseSizePx).toInt().coerceAtLeast(1)
    return columns * rows
}

internal fun shouldKeepSheep(
    sheep: SheepItem,
    screenWidth: Float,
    screenHeight: Float,
    playAreaStartY: Float,
    sheepBaseSizePx: Float
): Boolean {
    if (sheep.motionState != SheepMotionState.DRIFTING) return true
    val isBeyondLeft = sheep.x + sheepBaseSizePx <= 0f
    val isBeyondRight = sheep.x >= screenWidth
    val isBeyondTop = sheep.y + sheepBaseSizePx <= playAreaStartY
    val isBeyondBottom = sheep.y >= screenHeight
    return !(isBeyondLeft || isBeyondRight || isBeyondTop || isBeyondBottom)
}

private fun createDraggedSheepPreview(
    position: Offset,
    screenWidth: Float,
    screenHeight: Float,
    sheepBaseSizePx: Float,
    artwork: SheepArtwork,
    id: Int
): SheepItem? {
    if (screenWidth <= 0f || screenHeight <= 0f) return null

    val playAreaStartY = screenHeight * 0.35f
    return SheepItem(
        id = id,
        x = (position.x - sheepBaseSizePx / 2f).coerceIn(
            0f,
            maxOf(0f, screenWidth - sheepBaseSizePx)
        ),
        y = (position.y - sheepBaseSizePx / 2f).coerceIn(
            playAreaStartY,
            maxOf(playAreaStartY, screenHeight - sheepBaseSizePx)
        ),
        vx = 0f,
        vy = 0f,
        artwork = artwork,
        motionState = SheepMotionState.ACTIVE
    )
}
