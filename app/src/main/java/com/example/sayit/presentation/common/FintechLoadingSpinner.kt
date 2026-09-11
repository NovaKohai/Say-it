package com.example.sayit.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500

/**
 * High-end Fintech Dual-Arc Gradient Loading Spinner.
 * Features a glowing rotating gradient trail, subtle center breathing pulse,
 * and graceful reduced-motion fallback.
 */
@Composable
fun FintechLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 2.5.dp,
    primaryColor: Color = Emerald500,
    accentColor: Color = CyanAccent
) {
    val allowMotion = motionEnabled()

    val rotation = if (allowMotion) {
        val transition = rememberInfiniteTransition(label = "spinnerRotation")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spinAngle"
        )
        angle
    } else {
        0f
    }

    val pulseScale = if (allowMotion) {
        val pulseTransition = rememberInfiniteTransition(label = "spinnerPulse")
        val scale by pulseTransition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "corePulse"
        )
        scale
    } else {
        1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background subtle track
            drawArc(
                color = primaryColor.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Dynamic luminous sweep arc
            val sweepGradient = Brush.sweepGradient(
                listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.3f),
                    primaryColor,
                    accentColor
                )
            )

            val currentStartAngle = if (allowMotion) rotation else 45f
            val currentSweepAngle = if (allowMotion) 270f else 240f

            drawArc(
                brush = sweepGradient,
                startAngle = currentStartAngle,
                sweepAngle = currentSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Center radiant core bead
            val centerRadius = (strokePx * 0.75f) * pulseScale
            drawCircle(
                color = accentColor.copy(alpha = if (allowMotion) 0.85f else 0.7f),
                radius = centerRadius,
                center = Offset(this.size.width / 2f, this.size.height / 2f)
            )
        }
    }
}
