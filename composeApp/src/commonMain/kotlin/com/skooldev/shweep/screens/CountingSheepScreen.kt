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
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
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
    val scope = rememberCoroutineScope()
    
    // Session tracking
    val sessionStartTime = remember { Clock.System.now().toEpochMilliseconds() }
    
    val sheepBaseSize = 80.dp
    val sheepBaseSizePx = with(density) { sheepBaseSize.toPx() }
    val minScale = 0.1f
    val maxSheepBeforeShrink = 20
    
    // Handle dispose (screen dim/off) - save session synchronously to ensure it completes
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
    
    // Timer effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedTime++
        }
    }
    
    // Physics animation loop
    LaunchedEffect(sheepList.size) {
        while (true) {
            delay(8) // ~120fps for smoother animation
            
            val screenWidth = screenSize.width
            val screenHeight = screenSize.height
            val playAreaHeight = screenHeight * 0.5f // bottom 50% of screen
            
            if (screenWidth > 0 && screenHeight > 0) {
                // Calculate scale based on sheep count
                val targetScale = if (sheepList.size > maxSheepBeforeShrink) {
                    val excessSheep = sheepList.size - maxSheepBeforeShrink
                    val scaleFactor = (maxSheepBeforeShrink.toFloat() / sheepList.size.toFloat())
                    maxOf(minScale, scaleFactor)
                } else {
                    1f
                }
                
                // Update scales
                sheepList.forEach { sheep ->
                    sheep.scale = targetScale
                }
                
                val playAreaStartY = screenHeight * 0.67f // Bottom third starts at 2/3 from top
                
                // Update positions
                sheepList.forEach { sheep ->
                    val currentSize = sheepBaseSizePx * sheep.scale
                    
                    // Update position
                    sheep.x += sheep.vx
                    sheep.y += sheep.vy
                    
                    // Bounce off walls (left/right)
                    if (sheep.x <= 0) {
                        sheep.x = 0f
                        sheep.vx = kotlin.math.abs(sheep.vx)
                    } else if (sheep.x >= screenWidth - currentSize) {
                        sheep.x = screenWidth - currentSize
                        sheep.vx = -kotlin.math.abs(sheep.vx)
                    }
                    
                    // Bounce off top/bottom of play area (bottom 50% of screen)
                    if (sheep.y <= playAreaStartY) {
                        sheep.y = playAreaStartY
                        sheep.vy = kotlin.math.abs(sheep.vy)
                    } else if (sheep.y >= screenHeight - currentSize) {
                        sheep.y = screenHeight - currentSize
                        sheep.vy = -kotlin.math.abs(sheep.vy)
                    }
                }
                
                // Collision detection between sheep
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
                            // Calculate collision response
                            val nx = dx / distance
                            val ny = dy / distance
                            
                            // Exchange velocities (elastic collision)
                            val tempVx = sheep1.vx
                            val tempVy = sheep1.vy
                            sheep1.vx = sheep2.vx
                            sheep1.vy = sheep2.vy
                            sheep2.vx = tempVx
                            sheep2.vy = tempVy
                            
                            // Separate sheep to prevent overlap
                            val overlap = minDistance - distance
                            sheep1.x -= nx * overlap / 2
                            sheep1.y -= ny * overlap / 2
                            sheep2.x += nx * overlap / 2
                            sheep2.y += ny * overlap / 2
                        }
                    }
                }
                
                // Remove sheep that are too small (off screen)
                sheepList.removeAll { it.scale <= minScale + 0.01f }
            }
        }
    }
    
    val hours = elapsedTime / 3600
    val minutes = (elapsedTime % 3600) / 60
    val seconds = elapsedTime % 60
    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                screenSize = size.toSize()
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                    },
                    onDragEnd = {
                        isUserInteracting = true
                        
                        // Add new sheep on swipe up
                        val screenWidth = screenSize.width
                        val screenHeight = screenSize.height
                                                val playAreaStartY = screenHeight * 0.5f // Bottom 50% starts at 50% from top
                        val playAreaHeight = screenHeight * 0.5f
                        
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
        // Background image
        Image(
            painter = painterResource(Res.drawable.background_counting),
            contentDescription = Strings.CD_COUNTING_BACKGROUND,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Sheep images
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
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding)
        ) {
            // Top row with close button and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Close button
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
                
                // Stats card
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
                            text = String.format(Strings.TIME_FORMAT, timeString),
                            fontSize = Dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = String.format(Strings.SHEEP_COUNT_DISPLAY, sheepCount),
                            fontSize = Dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Swipe up indicator
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