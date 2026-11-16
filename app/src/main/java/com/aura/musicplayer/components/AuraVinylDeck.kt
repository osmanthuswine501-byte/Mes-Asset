package com.aura.musicplayer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.musicplayer.model.Track
import com.aura.musicplayer.theme.AuraCornerRadius

/**
 * Aura Flagship Hero Vinyl & Album Artwork Deck
 * Features:
 * - 3D elevated cosmic album sleeve with smooth 32dp corner radius.
 * - Sliding grooved vinyl record with realistic grooves, center label and continuous rotation.
 * - Live animated neon frequency spectrum bars overlay.
 * - Dolby Atmos spatial audio badge.
 */
@Composable
fun AuraVinylDeck(
    track: Track,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylRotation"
    )

    // Animated bar height amplitudes for audio spectrum
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar4"
    )

    // Vinyl slide out offset when playing
    val vinylOffsetXDp by animateFloatAsState(
        targetValue = if (isPlaying) 40f else 20f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "VinylSlide"
    )

    Box(
        modifier = modifier.size(width = size + 50.dp, height = size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Sliding Grooved Vinyl Record (Back Layer)
        Box(
            modifier = Modifier
                .size(size * 0.92f)
                .offset(x = vinylOffsetXDp.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF101016))
                .border(2.dp, Color(0xFF282736), CircleShape)
                .rotate(if (isPlaying) rotationAngle else 0f),
            contentAlignment = Alignment.Center
        ) {
            // Grooves Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = this.size.width / 2f

                // Concentric micro-grooves
                for (r in listOf(0.85f, 0.72f, 0.60f, 0.48f, 0.38f)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = maxRadius * r,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }
            }

            // Center Vinyl Label
            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFF00E5FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF101016))
                )
            }
        }

        // 2. Front 3D Cosmic Album Cover Sleeve
        Surface(
            modifier = Modifier
                .size(size * 0.92f)
                .offset(x = (-20).dp)
                .shadow(24.dp, RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased))
                .clip(RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased)),
            shape = RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased),
            color = Color(0xFF161324)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38236B),
                                Color(0xFF1C1338),
                                Color(0xFF0F0B1E)
                            ),
                            radius = 450f
                        )
                    )
                    .padding(18.dp)
            ) {
                // Top Header: Dolby Atmos Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AuraCornerRadius.Full))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(AuraCornerRadius.Full))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DOLBY ATMOS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    // Sparkle Accent
                    Text(text = "✨", fontSize = 14.sp)
                }

                // Middle: Neon Audio Equalizer Spectrum Bars
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(44.dp)
                        .fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = listOf(bar1, bar3, bar2, bar4, bar2, bar1, bar4)
                    val barColors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFFE040FB),
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFFE040FB),
                        Color(0xFF00E5FF)
                    )

                    bars.forEachIndexed { i, amp ->
                        val h = if (isPlaying) (44 * amp).dp.coerceAtLeast(6.dp) else 6.dp
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(h)
                                .clip(RoundedCornerShape(AuraCornerRadius.Full))
                                .background(barColors[i % barColors.size])
                        )
                    }
                }

                // Bottom: Track & Artist Label on Album
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = track.genre.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
