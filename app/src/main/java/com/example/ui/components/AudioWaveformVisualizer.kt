package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    height: Dp = 48.dp,
    brush: Brush = Brush.verticalGradient(
        colors = listOf(ElectricCyan, SpotifyGreenBright, SpotifyGreen)
    )
) {
    val transition = rememberInfiniteTransition(label = "waveform_anim")

    // Dynamic phase animations for wave heights
    val phase1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )

    val phase2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    val phase3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase3"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((totalWidth - totalSpacing) / barCount).coerceAtLeast(3.dp.toPx())

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val factor = when (i % 3) {
                0 -> phase1 * (0.5f + 0.5f * kotlin.math.sin(progress * Math.PI).toFloat())
                1 -> phase2 * (0.6f + 0.4f * kotlin.math.cos(progress * Math.PI).toFloat())
                else -> phase3 * (0.4f + 0.6f * kotlin.math.sin(progress * 2 * Math.PI).toFloat().coerceAtLeast(0.1f))
            }

            val finalHeightFraction = if (isPlaying) {
                factor.coerceIn(0.1f, 1.0f)
            } else {
                0.08f // resting flat baseline when paused
            }

            val barH = canvasHeight * finalHeightFraction
            val x = i * (barWidth + barSpacing)
            val y = (canvasHeight - barH) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
