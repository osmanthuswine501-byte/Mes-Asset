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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.components.AuraExpressiveSlider
import com.aura.musicplayer.components.AuraIcons
import com.aura.musicplayer.components.AuraPlaybackFloatingToolbar
import com.aura.musicplayer.components.AuraSliderSize
import com.aura.musicplayer.components.AuraSplitButton
import com.aura.musicplayer.components.AuraVinylDeck
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

    Box(modifier = modifier.fillMaxSize()) {
        // Ambient background color glow leaking from track
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Back, Split Button for Audio Quality, and Equalizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = AuraIcons.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // M3 Expressive Split Button (Lossless Master 24-bit)
                AuraSplitButton(
                    selectedOption = selectedAudioQuality,
                    options = AudioQuality.values().toList(),
                    onOptionSelected = { viewModel.selectAudioQuality(it) },
                    labelProvider = { "MASTER 24-BIT" },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                    }
                )

                // Equalizer Trigger
                IconButton(
                    onClick = { viewModel.setEqualizerOpen(true) },
                    modifier = Modifier.size(44.dp)
                ) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFF00E5FF))
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

            // 2. Hero 3D Cosmic Vinyl Deck
            AuraVinylDeck(
                track = currentTrack,
                isPlaying = isPlaying,
                size = 250.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Track Metadata
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTrack.title,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "TitleAnim"
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
                    label = "ArtistAnim"
                ) { artist ->
                    Text(
                        text = "$artist • ${currentTrack.releaseYear}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Luminous Cyan M3 Expressive Audio Scrubber
            Column(modifier = Modifier.fillMaxWidth()) {
                AuraExpressiveSlider(
                    value = progressFraction,
                    onValueChange = { fraction -> viewModel.seekToFraction(fraction) },
                    size = AuraSliderSize.S,
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    handleColor = Color(0xFF00E5FF)
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
                        text = "LOSSLESS FLAC",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = formatDuration(currentTrack.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Official M3 Expressive Floating Toolbar Deck
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

            // 6. Volume Control Slider (XS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    val repeatTint = if (repeatMode != RepeatMode.OFF) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outline
                    Icon(
                        imageVector = AuraIcons.Repeat,
                        contentDescription = "Cycle repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = AuraIcons.VolumeUp,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    AuraExpressiveSlider(
                        value = volume,
                        onValueChange = { viewModel.setVolume(it) },
                        size = AuraSliderSize.XS,
                        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                        handleColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
