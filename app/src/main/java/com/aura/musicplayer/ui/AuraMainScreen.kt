package com.aura.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.components.AuraFlowerVisualizer
import com.aura.musicplayer.components.AuraIcons
import com.aura.musicplayer.model.PlaybackStatus
import com.aura.musicplayer.theme.AuraCornerRadius
import com.aura.musicplayer.viewmodel.MusicViewModel

enum class MainNavDestination(val label: String) {
    NOW_PLAYING("Now Playing"),
    LIBRARY("Library")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraMainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(MainNavDestination.NOW_PLAYING) }
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playbackStatus by viewModel.playbackStatus.collectAsState()
    val progressMs by viewModel.progressMs.collectAsState()
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 720.dp

        if (isExpandedLayout) {
            // Adaptive Two-Pane Layout (M3 Canonical Supporting Pane / List-Detail)
            Row(modifier = Modifier.fillMaxSize()) {
                // Navigation Rail
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    NavigationRailItem(
                        selected = currentDestination == MainNavDestination.NOW_PLAYING,
                        onClick = { currentDestination = MainNavDestination.NOW_PLAYING },
                        icon = { Icon(AuraIcons.Play, contentDescription = "Now Playing") },
                        label = { Text("Player") }
                    )
                    NavigationRailItem(
                        selected = currentDestination == MainNavDestination.LIBRARY,
                        onClick = { currentDestination = MainNavDestination.LIBRARY },
                        icon = { Icon(AuraIcons.QueueMusic, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                }

                // Left Pane: Library & Tracks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onTrackClicked = { }
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Right Pane: Hero Now Playing
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                ) {
                    NowPlayingScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = MainNavDestination.LIBRARY }
                    )
                }
            }
        } else {
            // Standard Compact Layout (Phone)
            Scaffold(
                bottomBar = {
                    Column {
                        // Mini Player (Visible when in Library view)
                        AnimatedVisibility(
                            visible = currentDestination == MainNavDestination.LIBRARY,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it }
                        ) {
                            val progressFraction = if (currentTrack.durationMs > 0) {
                                (progressMs.toFloat() / currentTrack.durationMs).coerceIn(0f, 1f)
                            } else 0f

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(AuraCornerRadius.ExtraLarge))
                                    .clickable { currentDestination = MainNavDestination.NOW_PLAYING },
                                shape = RoundedCornerShape(AuraCornerRadius.ExtraLarge),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 6.dp,
                                shadowElevation = 6.dp
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AuraFlowerVisualizer(
                                            isPlaying = isPlaying,
                                            primaryColor = currentTrack.primaryColor,
                                            accentColor = currentTrack.accentColor,
                                            size = 40.dp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentTrack.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentTrack.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.togglePlayPause() },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) AuraIcons.Pause else AuraIcons.Play,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.skipToNextTrack() },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = AuraIcons.SkipNext,
                                                contentDescription = "Next Track",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Subtle progress line at bottom of mini player
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }

                        // M3 Expressive Navigation Bar
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp
                        ) {
                            NavigationBarItem(
                                selected = currentDestination == MainNavDestination.NOW_PLAYING,
                                onClick = { currentDestination = MainNavDestination.NOW_PLAYING },
                                icon = {
                                    Icon(
                                        imageVector = AuraIcons.Play,
                                        contentDescription = "Now Playing"
                                    )
                                },
                                label = { Text("Player") }
                            )

                            NavigationBarItem(
                                selected = currentDestination == MainNavDestination.LIBRARY,
                                onClick = { currentDestination = MainNavDestination.LIBRARY },
                                icon = {
                                    Icon(
                                        imageVector = AuraIcons.QueueMusic,
                                        contentDescription = "Library"
                                    )
                                },
                                label = { Text("Library") }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        MainNavDestination.NOW_PLAYING -> {
                            NowPlayingScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentDestination = MainNavDestination.LIBRARY }
                            )
                        }
                        MainNavDestination.LIBRARY -> {
                            LibraryScreen(
                                viewModel = viewModel,
                                onTrackClicked = { currentDestination = MainNavDestination.NOW_PLAYING }
                            )
                        }
                    }
                }
            }
        }

        // Modal Equalizer Bottom Sheet
        if (isEqualizerOpen) {
            EqualizerBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setEqualizerOpen(false) }
            )
        }
    }
}
