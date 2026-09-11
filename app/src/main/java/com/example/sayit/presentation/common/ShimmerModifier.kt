package com.example.sayit.presentation.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlin.math.hypot

/**
 * Reads animation state during drawing so loading does not recompose every frame.
 * Dynamically adapts to component dimensions and creates a luminous fintech sheen.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    if (!motionEnabled()) return@composed background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val midColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

    drawBehind {
        val width = size.width
        val height = size.height
        val diagonal = hypot(width.toDouble(), height.toDouble()).toFloat()
        val totalTravel = diagonal * 1.8f
        val startOffset = -diagonal * 0.4f + (progress * totalTravel)
        val band = (diagonal * 0.35f).coerceIn(200f, 500f)

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    midColor,
                    highlightColor,
                    midColor,
                    baseColor
                ),
                start = Offset(startOffset - band, startOffset - band),
                end = Offset(startOffset + band, startOffset + band)
            )
        )
    }
}
