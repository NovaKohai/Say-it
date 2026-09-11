package com.example.sayit.presentation.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.sayit.theme.CyanAccent
import com.example.sayit.theme.Emerald500

@Composable
fun AudioWaveVisualizer(
    isRecording: Boolean,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    val allowMotion = motionEnabled()

    // Subtle ambient breathing when recording
    val ambientJitter = if (isRecording && allowMotion) {
        val transition = rememberInfiniteTransition(label = "recording_wave")
        val value by transition.animateFloat(0.9f, 1.1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recording_breath")
        value
    } else 1f

    // Reactive RMS scaling for real audio levels (rmsLevel ranges 0.0 to 1.0)
    val reactiveMultiplier = if (rmsLevel > 0.05f) {
        (rmsLevel * 2.5f).coerceIn(0.8f, 2.5f)
    } else {
        if (isRecording) ambientJitter else 0.4f
    }

    val smoothMultiplier by animateFloatAsState(
        targetValue = reactiveMultiplier,
        animationSpec = tween(120, easing = EmilEasings.StrongEaseOut),
        label = "smooth_rms"
    )

    // Base heights for standard 7-bar wave
    val baseHeights = listOf(14f, 24f, 38f, 52f, 40f, 26f, 16f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeBrush = Brush.verticalGradient(listOf(CyanAccent, Emerald500))
        val idleBrush = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.outlineVariant
            )
        )

        baseHeights.forEachIndexed { index, base ->
            // Distribute multiplier slightly across bars for organic acoustic feel
            val barFactor = when (index) {
                3 -> 1.0f
                2, 4 -> 0.85f
                1, 5 -> 0.70f
                else -> 0.55f
            }
            val scaleFactor = if (isRecording && allowMotion) {
                (smoothMultiplier * barFactor).coerceIn(0.2f, 1.8f)
            } else {
                (8f / base).coerceIn(0.15f, 1f)
            }

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(base.dp)
                    .graphicsLayer {
                        scaleY = scaleFactor
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isRecording) activeBrush else idleBrush)
            )
        }
    }
}
