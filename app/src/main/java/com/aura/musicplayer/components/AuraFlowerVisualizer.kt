package com.aura.musicplayer.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aura Hero Morphing Flower / Sunny Visualizer
 * Directly inspired by the Aura M3 Expressive case study (foundations/usability/applying-m-3-expressive).
 * Morphs and breathes with music playback rhythm, amplitude, and rotation.
 */
@Composable
fun AuraFlowerVisualizer(
    isPlaying: Boolean,
    primaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    amplitude: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlowerBreathing")

    // Gentle continuous rotation when playing
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAnim"
    )

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScaleAnim"
    )

    // Dynamic scale driven by playback state and audio amplitude
    val targetScale = if (isPlaying) (pulseScale * (0.95f + amplitude * 0.15f)) else 0.88f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "AnimatedScale"
    )

    // Dynamic petal depth morphing
    val petalDepth by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PetalDepthAnim"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(animatedScale)
            .rotate(if (isPlaying) rotationAngle else 0f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val centerX = canvasSize.width / 2f
            val centerY = canvasSize.height / 2f
            val outerRadius = minOf(centerX, centerY) * 0.95f
            val innerRadius = outerRadius * (if (isPlaying) petalDepth else 0.84f)

            // 1. Draw outer glowing halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isPlaying) 0.35f else 0.15f),
                        primaryColor.copy(alpha = if (isPlaying) 0.18f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = outerRadius * 1.15f
                ),
                radius = outerRadius * 1.15f,
                center = Offset(centerX, centerY)
            )

            // 2. Draw 12-point Sunny Flower Path
            val petalCount = 12
            val totalPoints = petalCount * 2
            val angleStep = (2.0 * PI / totalPoints).toFloat()
            val points = mutableListOf<Offset>()

            for (i in 0 until totalPoints) {
                val angle = i * angleStep - (PI / 2).toFloat()
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                points.add(Offset(x, y))
            }

            if (points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in points.indices) {
                        val current = points[i]
                        val next = points[(i + 1) % points.size]
                        val prev = points[(i - 1 + points.size) % points.size]
                        val nextNext = points[(i + 2) % points.size]

                        val cp1x = current.x + (next.x - prev.x) * 0.45f
                        val cp1y = current.y + (next.y - prev.y) * 0.45f
                        val cp2x = next.x - (nextNext.x - current.x) * 0.45f
                        val cp2y = next.y - (nextNext.y - current.y) * 0.45f

                        cubicTo(cp1x, cp1y, cp2x, cp2y, next.x, next.y)
                    }
                    close()
                }

                // Fill flower with vibrant radial gradient
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.85f),
                            primaryColor
                        ),
                        center = Offset(centerX, centerY),
                        radius = outerRadius
                    ),
                    style = Fill
                )
            }

            // 3. Center Core Star Accent (Aura nucleus)
            val coreRadius = outerRadius * 0.16f
            drawCircle(
                color = primaryColor,
                radius = coreRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = coreRadius * 0.4f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
