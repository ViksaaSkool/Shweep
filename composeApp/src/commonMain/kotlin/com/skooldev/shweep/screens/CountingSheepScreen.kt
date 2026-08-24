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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import com.skooldev.shweep.data.ConsumeSheepResult
import com.skooldev.shweep.data.DailySheepQuota
import com.skooldev.shweep.data.DailySheepQuotaRepository
import com.skooldev.shweep.data.MockDailySheepQuotaRepository
import com.skooldev.shweep.data.MockSessionRepository
import com.skooldev.shweep.data.Session
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlin.math.floor
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MIN_UPWARD_DISTANCE_DP = 48f
private const val DRAG_ACTIVATION_DISTANCE_DP = 16f
private const val DRAG_ACTIVATION_DURATION_MILLIS = 150L
private const val SLOW_GESTURE_THRESHOLD_DP_PER_SECOND = 700f

private enum class SheepGestureMode {
    PENDING,
    DRAGGING
}

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun CountingSheepScreen(
    onBackClick: () -> Unit,
    sessionRepository: SessionRepository,
    dailySheepQuotaRepository: DailySheepQuotaRepository,
    sheepArtwork: SheepArtwork = SheepArtwork.WHITE
) {
    var isUserInteracting by remember { mutableStateOf(false) }
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

    val sessionStartTime: Long = remember { Clock.System.now().toEpochMilliseconds() }

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

    DisposableEffect(Unit) {
        onDispose {
            runBlocking {
                val endTime = Clock.System.now().toEpochMilliseconds()
                val session = Session(
                    id = Uuid.random().toString(),
                    startTime = sessionStartTime,
                    endTime = endTime,
                    sheepCount = sheepCount
                )
                sessionRepository.addSession(session)
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val deltaSeconds =
                        ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0).toFloat()
                            .coerceAtMost(SheepSimulation.MAX_DELTA_SECONDS)
                    lastFrameTimeNanos = frameTimeNanos
                    frameCounter++

                    val screenWidth = screenSize.width
                    val screenHeight = screenSize.height

                    if (screenWidth > 0 && screenHeight > 0 && deltaSeconds > 0f) {
                        val playAreaStartY = screenHeight * 0.35f

                        val stepped = SheepSimulation.step(
                            sheepList = sheepList.value,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            playAreaStartY = playAreaStartY,
                            sheepBaseSizePx = sheepBaseSizePx,
                            deltaSeconds = deltaSeconds
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
                        isUserInteracting = true
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
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                        velocityTracker.addPosition(
                            timeMillis = change.uptimeMillis,
                            position = change.position
                        )

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

                        if (screenWidth > 0 && playAreaHeight > 0) {
                            if (gestureMode == SheepGestureMode.DRAGGING && draggedSheep != null) {
                                val dropX = draggedSheep!!.x.coerceIn(0f, screenWidth - sheepBaseSizePx)
                                val dropY = draggedSheep!!.y.coerceIn(playAreaStartY, screenHeight - sheepBaseSizePx)
                                val capturedDropX = dropX
                                val capturedDropY = dropY

                                scope.launch {
                                    when (val result = dailySheepQuotaRepository.tryConsumeSheep()) {
                                        is ConsumeSheepResult.Allowed -> {
                                            val lifetime = randomLifetimeSeconds()
                                            val turnInterval = randomTurnIntervalSeconds()
                                            val droppedSheep = SheepItem(
                                                id = sheepCount,
                                                x = capturedDropX,
                                                y = capturedDropY,
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
                                            sheepList.value = updatedSheep + droppedSheep
                                            sheepCount++
                                        }
                                        is ConsumeSheepResult.Exhausted -> {
                                            exhaustedQuota = result.quota
                                        }
                                    }
                                }
                            } else {
                                val isUpwardSwipe = totalDragY < -minUpwardDistancePx
                                if (isUpwardSwipe) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val releaseUpwardSpeed = -velocity.y
                                    val averageUpwardSpeed = maxOf(0f, -totalDragY / maxOf(0.001f, (Clock.System.now().toEpochMilliseconds() - initialEventTimeMillis) / 1000f))
                                    val effectiveUpwardSpeed = maxOf(averageUpwardSpeed, releaseUpwardSpeed)

                                    if (effectiveUpwardSpeed >= slowGestureThresholdPxPerSecond) {
                                        scope.launch {
                                            when (val result = dailySheepQuotaRepository.tryConsumeSheep()) {
                                                is ConsumeSheepResult.Allowed -> {
                                                    val playAreaHeight = screenHeight - playAreaStartY
                                                    val updatedSheep = makeRoomForNewSheep(sheepList.value, meadowCapacity, screenWidth, sheepBaseSizePx)

                                                    val lifetime = randomLifetimeSeconds()
                                                    val turnInterval = randomTurnIntervalSeconds()
                                                    val newSheep = SheepItem(
                                                        id = sheepCount,
                                                        x = Random.nextFloat() * maxOf(0f, screenWidth - sheepBaseSizePx),
                                                        y = playAreaStartY + Random.nextFloat() * maxOf(0f, playAreaHeight - sheepBaseSizePx),
                                                        vx = (Random.nextFloat() - 0.5f) * 500f,
                                                        vy = (Random.nextFloat() - 0.5f) * 500f,
                                                        artwork = sheepArtwork,
                                                        gaitPhaseRadians = initialGaitPhase(sheepCount),
                                                        lifetimeSeconds = lifetime,
                                                        nextZigzagTurnIn = turnInterval,
                                                        zigzagTurnInterval = turnInterval
                                                    )
                                                    sheepList.value = updatedSheep + newSheep
                                                    sheepCount++
                                                }
                                                is ConsumeSheepResult.Exhausted -> {
                                                    exhaustedQuota = result.quota
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        draggedSheep = null
                        gestureMode = SheepGestureMode.PENDING
                    },
                    onDragCancel = {
                        draggedSheep = null
                        gestureMode = SheepGestureMode.PENDING
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(Res.drawable.background_counting),
            contentDescription = Strings.CD_COUNTING_BACKGROUND,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LayeredSheepCanvas(
            sheepList = sheepList.value,
            draggedSheep = draggedSheep,
            sheepBaseSizePx = sheepBaseSizePx,
            frameCounter = frameCounter,
            modifier = Modifier.fillMaxSize()
        )

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

            if (!isUserInteracting) {
                SwipeUpIndicator()
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
        }
    }

    exhaustedQuota?.let { quota ->
        OutOfSheepDialog(
            nextResetEpochMillis = quota.nextResetEpochMillis,
            onDismiss = { exhaustedQuota = null },
            onResetReached = {
                exhaustedQuota = null
                scope.launch { dailySheepQuotaRepository.refresh() }
            }
        )
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

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Preview
@Composable
fun CountingSheepScreenPreview() {
    CountingSheepScreen(
        onBackClick = {},
        sessionRepository = MockSessionRepository(),
        dailySheepQuotaRepository = MockDailySheepQuotaRepository()
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
