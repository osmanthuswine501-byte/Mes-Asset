package com.aura.musicplayer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.model.Track
import com.aura.musicplayer.theme.AuraCornerRadius

/**
 * Official Material 3 Expressive Multi-Browse Carousel (m3.material.io/components/carousel/specs).
 * Features dynamic item widths (large focused item, medium, and small preview items),
 * with smooth spring scaling and 2.5D container elevation.
 */
@Composable
fun AuraMultiBrowseCarousel(
    tracks: List<Track>,
    currentTrack: Track,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(tracks) { _, track ->
            val isSelected = track.id == currentTrack.id

            val itemScale by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.88f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
                label = "CarouselItemScale"
            )

            val itemWidth = if (isSelected) 220.dp else 160.dp
            val itemHeight = if (isSelected) 220.dp else 160.dp

            Surface(
                modifier = Modifier
                    .width(itemWidth)
                    .height(itemHeight)
                    .scale(itemScale)
                    .clip(RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased))
                    .clickable { onTrackSelected(track) },
                shape = RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = if (isSelected) 8.dp else 2.dp,
                shadowElevation = if (isSelected) 12.dp else 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    track.primaryColor,
                                    track.accentColor.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Decorative inner flower watermark
                    AuraFlowerVisualizer(
                        isPlaying = isSelected,
                        primaryColor = track.primaryColor.copy(alpha = 0.4f),
                        accentColor = track.accentColor.copy(alpha = 0.3f),
                        size = 110.dp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    Column {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
