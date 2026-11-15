package com.aura.musicplayer.components

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Official Material 3 Expressive Loading Indicator (m3.material.io/components/loading-indicator/specs).
 * Specifications:
 * - 48dp touch/clearance container.
 * - 38dp shape container with morphing geometry and continuous rotation.
 */
@Composable
fun AuraLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingMorph")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoadingRotation"
    )

    val cornerMorph by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CornerMorph"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            val canvasSize = this.size.width
            val cornerRadius = cornerMorph * (canvasSize / 38f)

            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(canvasSize, canvasSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
