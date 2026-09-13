package com.skooldev.shweep.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.tooling.preview.Preview
import com.skooldev.shweep.CountingSessionCoordinator
import com.skooldev.shweep.MonotonicClock
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import com.skooldev.shweep.data.ConsumeSheepResult
import com.skooldev.shweep.data.DailySheepQuota
import com.skooldev.shweep.data.DailySheepQuotaRepository
import com.skooldev.shweep.data.MockDailySheepQuotaRepository
import com.skooldev.shweep.data.MockSessionRepository
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.data.SheepAccessMode
import com.skooldev.shweep.data.resolveSheepAccessMode
import com.skooldev.shweep.purchase.UnlimitedSheepPurchaseState
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlin.math.floor
import kotlin.random.Random
import kotlin.time.ExperimentalTime

private const val MIN_UPWARD_DISTANCE_DP = 48f
private const val DRAG_ACTIVATION_DISTANCE_DP = 16f
private const val DRAG_ACTIVATION_DURATION_MILLIS = 150L
private const val SLOW_GESTURE_THRESHOLD_DP_PER_SECOND = 700f

private enum class SheepGestureMode {
    PENDING,
    DRAGGING
}

@OptIn(ExperimentalTime::class)
@Composable
fun CountingSheepScreen(
    onBackClick: () -> Unit,
    sessionRepository: SessionRepository,
    dailySheepQuotaRepository: DailySheepQuotaRepository,
    limitedSheepEnabled: Boolean,
    purchaseState: UnlimitedSheepPurchaseState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    sheepArtwork: SheepArtwork = SheepArtwork.WHITE,
    coordinator: CountingSessionCoordinator
) {
    val accessMode = remember(limitedSheepEnabled, purchaseState.entitlement) {
        resolveSheepAccessMode(limitedSheepEnabled, purchaseState.entitlement)
    }
    var sheepCount by remember { mutableStateOf(0) }
    var screenSize by remember { mutableStateOf(Size.Zero) }
    var frameCounter by remember { mutableIntStateOf(0) }
    val sheepList = remember { mutableStateOf<List<SheepItem>>(emptyList()) }
    val density = LocalDensity.current
    val densityScale = density.density
    val scope = rememberCoroutineScope()

    var draggedSheep by remember { mutableStateOf<SheepItem?>(null) }
    var gestureMode by remember { mutableStateOf(SheepGestureMode.PENDING) }
    var initialPointerY by remember { mutableFloatStateOf(0f) }
    var initialEventTimeMillis by remember { mutableLongStateOf(0L) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val velocityTracker = remember { VelocityTracker() }
    var exhaustedQuota by remember { mutableStateOf<DailySheepQuota?>(null) }

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
                when (val result = dailySheepQuotaRepository.tryConsumeSheep()) {
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
                    }
                    is ConsumeSheepResult.Exhausted -> {
                        coordinator.recordAttempt(sample, elapsedMillis)
                        exhaustedQuota = result.quota
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
                            deltaSeconds = physicsDeltaSeconds
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
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureMode = SheepGestureMode.PENDING
                        initialPointerY = offset.y
                        initialEventTimeMillis = Clock.System.now().toEpochMilliseconds()
                        totalDragY = 0f
                        draggedSheep = null
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(
                            timeMillis = Clock.System.now().toEpochMilliseconds(),
                            position = offset
                        )
                        coordinator.onPointerActivity(MonotonicClock.nowMillis())
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                        velocityTracker.addPosition(
                            timeMillis = change.uptimeMillis,
                            position = change.position
                        )
                        coordinator.onPointerActivity(MonotonicClock.nowMillis())

                        if (gestureMode == SheepGestureMode.PENDING) {
                            val currentUpwardDistance = maxOf(0f, initialPointerY - change.position.y)
                            val elapsedMillis = Clock.System.now().toEpochMilliseconds() - initialEventTimeMillis
                            val isUpward = totalDragY < 0f

                            if (isUpward && currentUpwardDistance >= dragActivationDistancePx) {
                                val gestureDurationSeconds = (elapsedMillis.coerceAtLeast(1)) / 1000f
                                val averageUpwardSpeed = currentUpwardDistance / gestureDurationSeconds

                                if (elapsedMillis >= DRAG_ACTIVATION_DURATION_MILLIS && averageUpwardSpeed < slowGestureThresholdPxPerSecond) {
                                    gestureMode = SheepGestureMode.DRAGGING
                                    val previewX = change.position.x - sheepBaseSizePx / 2f
                                    val previewY = change.position.y - sheepBaseSizePx / 2f
                                    draggedSheep = SheepItem(
                                        id = sheepCount,
                                        x = previewX.coerceIn(0f, screenSize.width - sheepBaseSizePx),
                                        y = previewY.coerceIn(screenSize.height * 0.35f, screenSize.height - sheepBaseSizePx),
                                        vx = 0f,
                                        vy = 0f,
                                        artwork = sheepArtwork,
                                        motionState = SheepMotionState.ACTIVE
                                    )
                                }
                            }
                        }

                        if (gestureMode == SheepGestureMode.DRAGGING) {
                            val screenWidth = screenSize.width
                            val screenHeight = screenSize.height
                            if (screenWidth > 0 && screenHeight > 0) {
                                val previewX = change.position.x - sheepBaseSizePx / 2f
                                val previewY = change.position.y - sheepBaseSizePx / 2f
                                val playAreaStartY = screenHeight * 0.35f
                                draggedSheep = SheepItem(
                                    id = sheepCount,
                                    x = previewX.coerceIn(0f, screenWidth - sheepBaseSizePx),
                                    y = previewY.coerceIn(playAreaStartY, screenHeight - sheepBaseSizePx),
                                    vx = 0f,
                                    vy = 0f,
                                    artwork = sheepArtwork,
                                    motionState = SheepMotionState.ACTIVE
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        val screenWidth = screenSize.width
                        val screenHeight = screenSize.height
                        val playAreaStartY = screenHeight * 0.35f
                        val playAreaHeight = screenHeight - playAreaStartY
                        val now = MonotonicClock.nowMillis()

                        if (screenWidth > 0 && playAreaHeight > 0) {
                            if (gestureMode == SheepGestureMode.DRAGGING && draggedSheep != null) {
                                val dropX = draggedSheep!!.x.coerceIn(0f, screenWidth - sheepBaseSizePx)
                                val dropY = draggedSheep!!.y.coerceIn(playAreaStartY, screenHeight - sheepBaseSizePx)

                                val elapsed = Clock.System.now().toEpochMilliseconds() - initialEventTimeMillis
                                val distance = maxOf(0f, -totalDragY)
                                val speed = if (elapsed > 0) distance / (elapsed / 1000f) else 0f

                                val sample = GestureSample(
                                    mode = GestureMode.DRAG,
                                    durationMillis = elapsed,
                                    upwardDistanceDp = distance / densityScale,
                                    effectiveSpeedDpPerSecond = speed / densityScale,
                                    completed = true,
                                    cancelled = false
                                )

                                createSheep(sample, dropX, dropY, screenWidth, sheepBaseSizePx, now)
                            } else {
                                val isUpwardSwipe = totalDragY < -minUpwardDistancePx
                                if (isUpwardSwipe) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val releaseUpwardSpeed = -velocity.y
                                    val elapsed = Clock.System.now().toEpochMilliseconds() - initialEventTimeMillis
                                    val averageUpwardSpeed = maxOf(0f, -totalDragY / maxOf(0.001f, elapsed / 1000f))
                                    val effectiveUpwardSpeed = maxOf(averageUpwardSpeed, releaseUpwardSpeed)

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

                                        val newX = Random.nextFloat() * maxOf(0f, screenWidth - sheepBaseSizePx)
                                        val newY = playAreaStartY + Random.nextFloat() * maxOf(0f, playAreaHeight - sheepBaseSizePx)
                                        createSheep(sample, newX, newY, screenWidth, sheepBaseSizePx, now)
                                    } else {
                                        coordinator.recordAttempt(
                                            GestureSample(
                                                mode = GestureMode.FLICK,
                                                durationMillis = elapsed,
                                                upwardDistanceDp = -totalDragY / densityScale,
                                                effectiveSpeedDpPerSecond = effectiveUpwardSpeed / densityScale,
                                                completed = false,
                                                cancelled = false
                                            ),
                                            now
                                        )
                                    }
                                }
                            }
                        }
                        draggedSheep = null
                        gestureMode = SheepGestureMode.PENDING
                    },
                    onDragCancel = {
                        val elapsed = Clock.System.now().toEpochMilliseconds() - initialEventTimeMillis
                        coordinator.recordAttempt(
                            GestureSample(
                                mode = GestureMode.DRAG,
                                durationMillis = elapsed,
                                upwardDistanceDp = 0f,
                                effectiveSpeedDpPerSecond = 0f,
                                completed = false,
                                cancelled = true
                            ),
                            MonotonicClock.nowMillis()
                        )
                        draggedSheep = null
                        gestureMode = SheepGestureMode.PENDING
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.background_counting),
                contentDescription = Strings.CD_COUNTING_BACKGROUND,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(sceneColorMatrix)
            )

            LayeredSheepCanvas(
                sheepList = sheepList.value,
                draggedSheep = draggedSheep,
                sheepBaseSizePx = sheepBaseSizePx,
                frameCounter = frameCounter,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.colorMatrix(sceneColorMatrix)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(top = Dimens.spacingMedium)
                ) {
                    Text(
                        text = Strings.HISTORY_CLOSE,
                        fontSize = Dimens.fontSizeXLarge,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (sheepCount == 0) {
                SwipeUpIndicator()
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
        }
    }

    LaunchedEffect(limitedSheepEnabled, purchaseState.isPurchased) {
        if (!limitedSheepEnabled || purchaseState.isPurchased) {
            exhaustedQuota = null
        }
    }

    exhaustedQuota?.let { quota ->
        if (accessMode == SheepAccessMode.LIMITED) {
            OutOfSheepDialog(
                nextResetEpochMillis = quota.nextResetEpochMillis,
                purchaseState = purchaseState,
                onPurchase = onPurchase,
                onRestore = onRestore,
                onDismiss = { exhaustedQuota = null },
                onResetReached = {
                    exhaustedQuota = null
                    scope.launch { dailySheepQuotaRepository.refresh() }
                }
            )
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

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun CountingSheepScreenPreview() {
    CountingSheepScreen(
        onBackClick = {},
        sessionRepository = MockSessionRepository(),
        dailySheepQuotaRepository = MockDailySheepQuotaRepository(),
        limitedSheepEnabled = false,
        purchaseState = UnlimitedSheepPurchaseState(),
        onPurchase = {},
        onRestore = {},
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
