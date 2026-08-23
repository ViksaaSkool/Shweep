package com.skooldev.shweep.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import com.skooldev.shweep.data.MockSessionRepository
import com.skooldev.shweep.data.Session
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun CountingSheepScreen(
    onBackClick: () -> Unit,
    sessionRepository: SessionRepository
) {
    var isUserInteracting by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0) }
    var sheepCount by remember { mutableStateOf(0) }
    var screenSize by remember { mutableStateOf(Size.Zero) }
    var frameCounter by remember { mutableIntStateOf(0) }
    val sheepList = remember { mutableStateOf<List<SheepItem>>(emptyList()) }
    val density = LocalDensity.current

    val sessionStartTime: Long = remember { Clock.System.now().toEpochMilliseconds() }

    val sheepBaseSize = 80.dp
    val sheepBaseSizePx = with(density) { sheepBaseSize.toPx() }
    val minScale = 0.1f
    val maxSheepBeforeShrink = 20

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
        while (true) {
            delay(1000)
            elapsedTime++
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

                        val targetScale = if (sheepList.value.size > maxSheepBeforeShrink) {
                            val scaleFactor =
                                maxSheepBeforeShrink.toFloat() / sheepList.value.size.toFloat()
                            maxOf(minScale, scaleFactor)
                        } else 1f

                        val stepped = SheepSimulation.step(
                            sheepList = sheepList.value,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            playAreaStartY = playAreaStartY,
                            sheepBaseSizePx = sheepBaseSizePx,
                            targetScale = targetScale,
                            deltaSeconds = deltaSeconds
                        )

                        sheepList.value = stepped.filter { it.scale > minScale + 0.01f }
                    }
                } else {
                    lastFrameTimeNanos = frameTimeNanos
                }
            }
        }
    }

    val hours = elapsedTime / 3600
    val minutes = (elapsedTime % 3600) / 60
    val seconds = elapsedTime % 60
    val timeString = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> screenSize = size.toSize() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ -> change.consume() },
                    onDragEnd = {
                        isUserInteracting = true

                        val screenWidth = screenSize.width
                        val screenHeight = screenSize.height
                        val playAreaStartY = screenHeight * 0.35f
                        val playAreaHeight = screenHeight * 0.65f

                        if (screenWidth > 0 && playAreaHeight > 0) {
                            val newSheep = SheepItem(
                                id = sheepCount,
                                x = Random.nextFloat() * (screenWidth - sheepBaseSizePx),
                                y = playAreaStartY + Random.nextFloat() * (playAreaHeight - sheepBaseSizePx),
                                vx = (Random.nextFloat() - 0.5f) * 500f,
                                vy = (Random.nextFloat() - 0.5f) * 500f,
                                artwork = SheepArtwork.WHITE,
                                // Golden-angle spacing keeps flock legs out of sync.
                                gaitPhaseRadians = SheepGait.positiveModulo(
                                    sheepCount * 2.3999632f,
                                    SheepGait.TAU
                                )
                            )
                            sheepList.value += newSheep
                            sheepCount++
                        }
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

                Card(
                    modifier = Modifier.padding(top = Dimens.spacingLarge),
                    shape = RoundedCornerShape(Dimens.cardCornerRadiusLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.CardBackgroundMediumAlpha
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimens.paddingXLarge,
                            vertical = Dimens.paddingMedium
                        )
                    ) {
                        Text(
                            text = "Time: $timeString",
                            fontSize = Dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Sheep: $sheepCount",
                            fontSize = Dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isUserInteracting) {
                SwipeUpIndicator()
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXXXLarge))
        }
    }
}

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
        sessionRepository = MockSessionRepository()
    )
}