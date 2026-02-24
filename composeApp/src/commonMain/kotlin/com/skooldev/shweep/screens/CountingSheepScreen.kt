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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import shweep.composeapp.generated.resources.Res
import shweep.composeapp.generated.resources.background_counting
import shweep.composeapp.generated.resources.sheep
import com.skooldev.shweep.data.Session
import com.skooldev.shweep.data.SessionRepository
import com.skooldev.shweep.ui.theme.Dimens
import com.skooldev.shweep.ui.theme.AppColors
import com.skooldev.shweep.ui.theme.Strings
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun CountingSheepScreen(
    onBackClick: () -> Unit,
    sessionRepository: SessionRepository
) {
    var isUserInteracting by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0) }
    var sheepCount by remember { mutableStateOf(0) }
    var screenSize by remember { mutableStateOf(Size.Zero) }
    val sheepList = remember { mutableStateListOf<SheepItem>() }
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

    LaunchedEffect(sheepList.size) {
        while (true) {
            delay(8)

            val screenWidth = screenSize.width
            val screenHeight = screenSize.height

            if (screenWidth > 0 && screenHeight > 0) {
                val targetScale = if (sheepList.size > maxSheepBeforeShrink) {
                    val scaleFactor = maxSheepBeforeShrink.toFloat() / sheepList.size.toFloat()
                    maxOf(minScale, scaleFactor)
                } else 1f

                sheepList.forEach { sheep -> sheep.scale = targetScale }

                val playAreaStartY = screenHeight * 0.35f

                sheepList.forEach { sheep ->
                    val currentSize = sheepBaseSizePx * sheep.scale

                    sheep.x += sheep.vx
                    sheep.y += sheep.vy

                    if (sheep.x <= 0) {
                        sheep.x = 0f
                        sheep.vx = kotlin.math.abs(sheep.vx)
                    } else if (sheep.x >= screenWidth - currentSize) {
                        sheep.x = screenWidth - currentSize
                        sheep.vx = -kotlin.math.abs(sheep.vx)
                    }

                    if (sheep.y <= playAreaStartY) {
                        sheep.y = playAreaStartY
                        sheep.vy = kotlin.math.abs(sheep.vy)
                    } else if (sheep.y >= screenHeight - currentSize) {
                        sheep.y = screenHeight - currentSize
                        sheep.vy = -kotlin.math.abs(sheep.vy)
                    }
                }

                for (i in sheepList.indices) {
                    for (j in i + 1 until sheepList.size) {
                        val sheep1 = sheepList[i]
                        val sheep2 = sheepList[j]

                        val dx = (sheep2.x + sheepBaseSizePx * sheep2.scale / 2) -
                                (sheep1.x + sheepBaseSizePx * sheep1.scale / 2)
                        val dy = (sheep2.y + sheepBaseSizePx * sheep2.scale / 2) -
                                (sheep1.y + sheepBaseSizePx * sheep1.scale / 2)
                        val distance = sqrt(dx * dx + dy * dy)
                        val minDistance = (sheepBaseSizePx * sheep1.scale + sheepBaseSizePx * sheep2.scale) / 2

                        if (distance < minDistance && distance > 0) {
                            val nx = dx / distance
                            val ny = dy / distance

                            val tempVx = sheep1.vx
                            val tempVy = sheep1.vy
                            sheep1.vx = sheep2.vx
                            sheep1.vy = sheep2.vy
                            sheep2.vx = tempVx
                            sheep2.vy = tempVy

                            val overlap = minDistance - distance
                            sheep1.x -= nx * overlap / 2
                            sheep1.y -= ny * overlap / 2
                            sheep2.x += nx * overlap / 2
                            sheep2.y += ny * overlap / 2
                        }
                    }
                }

                sheepList.removeAll { it.scale <= minScale + 0.01f }
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
                                vx = (Random.nextFloat() - 0.5f) * 4f,
                                vy = (Random.nextFloat() - 0.5f) * 4f
                            )
                            sheepList.add(newSheep)
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

        sheepList.forEach { sheep ->
            val currentSize = sheepBaseSize * sheep.scale
            Image(
                painter = painterResource(Res.drawable.sheep),
                contentDescription = "Sheep ${sheep.id}",
                modifier = Modifier
                    .size(currentSize)
                    .offset(
                        x = with(density) { sheep.x.toDp() },
                        y = with(density) { sheep.y.toDp() }
                    ),
                contentScale = ContentScale.Fit
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
            imageVector = Icons.Default.KeyboardArrowUp,
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