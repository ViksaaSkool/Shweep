package com.skooldev.shweep

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

internal const val SCREEN_TRANSITION_WINDOW_MILLIS = 400L

private const val MEADOW_ENTER_MILLIS = 320
private const val MEADOW_EXIT_MILLIS = 200
private const val IOS_SLIDE_MILLIS = 280
private const val ANDROID_FADE_OUT_MILLIS = 90
private const val ANDROID_FADE_IN_MILLIS = 130
private const val REDUCED_MOTION_FADE_MILLIS = 120

internal fun instantScreenContentTransform(): ContentTransform = ContentTransform(
    targetContentEnter = EnterTransition.None,
    initialContentExit = ExitTransition.None,
    sizeTransform = null
)

/**
 * Screen motion that feels native on each platform.
 *
 * The meadow always uses a quiet dissolve, while the lighter utility screens
 * use Material fade-through on Android and a navigation push/pop on iOS.
 */
internal fun screenContentTransform(
    initial: Screen,
    target: Screen,
    style: ScreenTransitionStyle,
    reduceMotion: Boolean
): ContentTransform {
    if (reduceMotion) {
        return ContentTransform(
            targetContentEnter = fadeIn(
                animationSpec = tween(durationMillis = REDUCED_MOTION_FADE_MILLIS)
            ),
            initialContentExit = fadeOut(
                animationSpec = tween(durationMillis = REDUCED_MOTION_FADE_MILLIS)
            ),
            sizeTransform = null
        )
    }

    val involvesMeadow = initial == Screen.Counting || target == Screen.Counting
    if (involvesMeadow) {
        val durationMillis = if (target == Screen.Counting) {
            MEADOW_ENTER_MILLIS
        } else {
            MEADOW_EXIT_MILLIS
        }
        return ContentTransform(
            targetContentEnter = fadeIn(
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            ),
            initialContentExit = fadeOut(
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutLinearInEasing
                )
            ),
            sizeTransform = null
        )
    }

    return when (style) {
        ScreenTransitionStyle.ANDROID -> androidFadeThroughTransform()
        ScreenTransitionStyle.IOS -> iosPushTransform(initial = initial)
    }
}

private fun androidFadeThroughTransform(): ContentTransform {
    val enterSpec = tween<Float>(
        durationMillis = ANDROID_FADE_IN_MILLIS,
        delayMillis = ANDROID_FADE_OUT_MILLIS,
        easing = LinearOutSlowInEasing
    )

    return ContentTransform(
        targetContentEnter = fadeIn(animationSpec = enterSpec) +
            scaleIn(initialScale = 0.98f, animationSpec = enterSpec),
        initialContentExit = fadeOut(
            animationSpec = tween(
                durationMillis = ANDROID_FADE_OUT_MILLIS,
                easing = FastOutLinearInEasing
            )
        ),
        sizeTransform = null
    )
}

private fun iosPushTransform(initial: Screen): ContentTransform {
    val isPush = initial == Screen.Start
    val enter = slideInHorizontally(
        animationSpec = tween(
            durationMillis = IOS_SLIDE_MILLIS,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { width -> if (isPush) width else -width / 3 }
    ) + fadeIn(
        animationSpec = tween(durationMillis = IOS_SLIDE_MILLIS)
    )

    val exit = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = IOS_SLIDE_MILLIS,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { width -> if (isPush) -width / 3 else width }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = IOS_SLIDE_MILLIS,
            easing = FastOutLinearInEasing
        )
    )

    return ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        sizeTransform = null
    )
}
