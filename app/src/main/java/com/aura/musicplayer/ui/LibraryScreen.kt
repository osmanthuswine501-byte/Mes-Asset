package com.aura.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.components.AuraConnectedButtonGroup
import com.aura.musicplayer.components.AuraFlowerVisualizer
import com.aura.musicplayer.components.AuraIcons
import com.aura.musicplayer.components.AuraMultiBrowseCarousel
import com.aura.musicplayer.model.PlaybackStatus
import com.aura.musicplayer.model.Track
import com.aura.musicplayer.theme.AuraCornerRadius
import com.aura.musicplayer.viewmodel.MusicViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onTrackClicked: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playbackStatus by viewModel.playbackStatus.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsState()
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING

    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Playlists", "Albums", "Artists", "Favorites")

    val filteredTracks = viewModel.playlist.filter { track ->
        val matchesCategory = when (selectedCategory) {
            "Favorites" -> favoriteTrackIds.contains(track.id)
            else -> true
        }
        val matchesSearch = track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true) ||
                track.album.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. Search Bar with M3 Expressive Rounded Pill Container
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Full)),
                placeholder = {
                    Text(
                        "Search tracks, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = AuraIcons.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // 2. M3 Expressive Connected Button Group (Filters)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            AuraConnectedButtonGroup(
                items = categories,
                selectedItem = selectedCategory,
                onItemSelected = { viewModel.selectCategory(it) },
                labelProvider = { it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Main List with Header Carousel & Expressive Items
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Featured Multi-Browse Carousel Section
            item {
                Column {
                    Text(
                        text = "Featured Atmospheres",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    AuraMultiBrowseCarousel(
                        tracks = viewModel.playlist,
                        currentTrack = currentTrack,
                        onTrackSelected = { track ->
                            viewModel.selectTrack(track)
                            onTrackClicked(track)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Track List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Releases",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredTracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Expressive List Items
            itemsIndexed(filteredTracks) { index, track ->
                val isCurrent = track.id == currentTrack.id
                val isFav = favoriteTrackIds.contains(track.id)

                ExpressiveTrackListItem(
                    track = track,
                    index = index + 1,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    isFavorite = isFav,
                    onItemClick = {
                        viewModel.selectTrack(track)
                        onTrackClicked(track)
                    },
                    onFavoriteClick = {
                        viewModel.toggleFavorite(track.id)
                    }
                )
            }
        }
    }
}

@Composable
fun ExpressiveTrackListItem(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onItemClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .background(containerBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Track Art / Morphing Flower Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Medium))
                    .background(
                        Brush.linearGradient(
                            listOf(track.primaryColor, track.accentColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    AuraFlowerVisualizer(
                        isPlaying = isPlaying,
                        primaryColor = track.primaryColor,
                        accentColor = track.accentColor,
                        size = 36.dp
                    )
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration & Favorite Heart
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = AuraIcons.Favorite,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
