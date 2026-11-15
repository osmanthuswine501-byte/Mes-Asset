package com.aura.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.components.AuraExpressiveSlider
import com.aura.musicplayer.components.AuraFlowerVisualizer
import com.aura.musicplayer.components.AuraIcons
import com.aura.musicplayer.components.AuraPlaybackFloatingToolbar
import com.aura.musicplayer.components.AuraSliderSize
import com.aura.musicplayer.components.AuraSplitButton
import com.aura.musicplayer.model.AudioQuality
import com.aura.musicplayer.model.PlaybackStatus
import com.aura.musicplayer.model.RepeatMode
import com.aura.musicplayer.viewmodel.MusicViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playbackStatus by viewModel.playbackStatus.collectAsState()
    val progressMs by viewModel.progressMs.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsState()
    val selectedAudioQuality by viewModel.selectedAudioQuality.collectAsState()
    val isFavorite = favoriteTrackIds.contains(currentTrack.id)
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING

    val scrollState = rememberScrollState()

    val progressFraction = if (currentTrack.durationMs > 0) {
        (progressMs.toFloat() / currentTrack.durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Bar: Back Action, Audio Quality Split Button, and Equalizer Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = AuraIcons.ArrowBack,
                    contentDescription = "Back to Library",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // M3 Expressive Split Button for Audio Quality
            AuraSplitButton(
                selectedOption = selectedAudioQuality,
                options = AudioQuality.values().toList(),
                onOptionSelected = { viewModel.selectAudioQuality(it) },
                labelProvider = { it.label.substringBefore(" ") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            )

            // Equalizer Button with Active Badge
            IconButton(
                onClick = { viewModel.setEqualizerOpen(true) },
                modifier = Modifier.size(48.dp)
            ) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary)
                    }
                ) {
                    Icon(
                        imageVector = AuraIcons.Equalizer,
                        contentDescription = "Open Equalizer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Hero Visualizer: Aura 12-lobed Morphing Flower
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            AuraFlowerVisualizer(
                isPlaying = isPlaying,
                primaryColor = currentTrack.primaryColor,
                accentColor = currentTrack.accentColor,
                size = 260.dp,
                amplitude = if (isPlaying) 0.65f else 0.2f
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Track Metadata with M3 Expressive Emphasized Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentTrack.title,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TrackTitleAnim"
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = currentTrack.artist,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TrackArtistAnim"
            ) { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = currentTrack.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. M3 Expressive Audio Scrubber (Track Slider)
        Column(modifier = Modifier.fillMaxWidth()) {
            AuraExpressiveSlider(
                value = progressFraction,
                onValueChange = { fraction -> viewModel.seekToFraction(fraction) },
                size = AuraSliderSize.S,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                handleColor = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(progressMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(currentTrack.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Official M3 Expressive Floating Toolbar with Playback Controls
        AuraPlaybackFloatingToolbar(
            isPlaying = isPlaying,
            isShuffle = isShuffle,
            isFavorite = isFavorite,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onPreviousClick = { viewModel.skipToPreviousTrack() },
            onNextClick = { viewModel.skipToNextTrack() },
            onShuffleClick = { viewModel.toggleShuffle() },
            onFavoriteClick = { viewModel.toggleFavorite() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Volume Control & Repeat Mode Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Repeat Mode Button
            val repeatTint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            IconButton(
                onClick = { viewModel.cycleRepeatMode() },
                modifier = Modifier.size(44.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (repeatMode == RepeatMode.ONE) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("1", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = AuraIcons.Repeat,
                        contentDescription = "Cycle repeat mode",
                        tint = repeatTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Volume Control Slider (XS configuration)
            Icon(
                imageVector = AuraIcons.VolumeUp,
                contentDescription = "Volume",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                AuraExpressiveSlider(
                    value = volume,
                    onValueChange = { viewModel.setVolume(it) },
                    size = AuraSliderSize.XS,
                    activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    handleColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
