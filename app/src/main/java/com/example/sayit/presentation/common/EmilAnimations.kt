package com.example.sayit.presentation.common

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Emil Kowalski Animation Philosophy & Micro-Interactions
 *
 * Core tenets:
 * 1. Buttons and pressables must feel tactile: scale(0.97) on press with natural spring physics.
 * 2. Never animate from scale(0): always start from scale(0.95) with opacity 0.
 * 3. Strong Ease-Out: Starts fast, feels instant (<250ms), settles smoothly.
 * 4. Hardware accelerated: Only animate transform and opacity on GPU via graphicsLayer.
 */
object EmilEasings {
    /**
     * Strong ease-out curve (cubic-bezier(0.23, 1, 0.32, 1)).
     * Starts with high initial velocity so the user immediately sees movement,
     * then gently decelerates.
     */
    val StrongEaseOut = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)

    /**
     * Symmetrical curve for on-screen morphs and transitions.
     */
    val StrongEaseInOut = CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)

    /**
     * Snappy spring with low bounce for UI micro-interactions.
     */
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Fast spring for immediate touch down / touch up feedback.
     */
    val FastPressSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 500f
    )
}

/**
 * Emil Kowalski tactile press feedback modifier with sensory haptics.
 * Scales down subtly (e.g. 0.97f or 0.95f) on touch down, provides a physical haptic tick,
 * and springs back smoothly on release.
 * Hardware-accelerated via graphicsLayer with zero layout re-measurement.
 */
fun Modifier.pressScale(
    targetScale: Float = 0.97f,
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && haptic) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = EmilEasings.FastPressSpring,
        label = "emilPressScale"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null, // Custom scale provides the primary tactile feedback
            onClick = {
                if (haptic) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
    } else {
        Modifier
    }

    this
        .then(clickableModifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Smooth Animated Numeric Text counter.
 * Morphs integer numbers smoothly over 350ms instead of abrupt visual jumps,
 * creating a perceptual feeling of living, real-time financial balance.
 */
@Composable
fun AnimatedNumericText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = 240,
            easing = EmilEasings.StrongEaseOut
        ),
        label = "numericCounter"
    )

    Text(
        text = "$prefix$animatedValue$suffix",
        style = style.copy(fontWeight = fontWeight),
        color = color,
        modifier = modifier
    )
}

/**
 * Natural Emil entrance transition:
 * Starts from scale(0.95f) (NEVER from 0!) + fadeIn + subtle vertical rise.
 */
fun emilScaleFadeEnter(
    durationMillis: Int = 220,
    initialScale: Float = 0.95f
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis, easing = EmilEasings.StrongEaseOut)
    ) + scaleIn(
        initialScale = initialScale,
        animationSpec = EmilEasings.SnappySpring
    )
}

/**
 * Snappy exit transition:
 * Quicker than enter (160ms) to ensure the interface never feels sluggish when dismissing.
 */
fun emilScaleFadeExit(
    durationMillis: Int = 160,
    targetScale: Float = 0.96f
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis, easing = EmilEasings.StrongEaseOut)
    ) + scaleOut(
        targetScale = targetScale,
        animationSpec = tween(durationMillis = durationMillis, easing = EmilEasings.StrongEaseOut)
    )
}
