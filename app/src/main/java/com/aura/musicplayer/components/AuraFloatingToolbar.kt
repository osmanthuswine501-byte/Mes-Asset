package com.aura.musicplayer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.theme.AuraCornerRadius

/**
 * Official Material 3 Expressive Floating Toolbar (m3.material.io/components/toolbars/specs).
 * Specifications:
 * - Height: 64dp
 * - Corner Radius: 32dp (Full Pill)
 * - Margins/Padding: 8dp horizontal, 4dp item spacing
 * - Hero Play/Pause Button: 56dp FAB container with spring physics bounce
 * - Secondary controls: Shuffle, Previous, Next, Favorite (48x48dp touch target)
 */
@Composable
fun AuraPlaybackFloatingToolbar(
    isPlaying: Boolean,
    isShuffle: Boolean,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased)),
        shape = RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Shuffle Toggle Button (48x48dp target)
            val shuffleInteraction = remember { MutableInteractionSource() }
            val shuffleTint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val shuffleBg = if (isShuffle) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shuffleBg)
                    .clickable(
                        interactionSource = shuffleInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onShuffleClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AuraIcons.Shuffle,
                    contentDescription = if (isShuffle) "Shuffle On" else "Shuffle Off",
                    tint = shuffleTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Previous Track Button
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = AuraIcons.SkipPrevious,
                    contentDescription = "Previous Track",
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. Hero Play / Pause Button (M3 Expressive Spring Morphing FAB)
            val playInteraction = remember { MutableInteractionSource() }
            val isPressed by playInteraction.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
                label = "PlayBounce"
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onPlayPauseClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) AuraIcons.Pause else AuraIcons.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            // 4. Next Track Button
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = AuraIcons.SkipNext,
                    contentDescription = "Next Track",
                    modifier = Modifier.size(26.dp)
                )
            }

            // 5. Favorite Button
            val favInteraction = remember { MutableInteractionSource() }
            val favTint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            val favBg = if (isFavorite) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else Color.Transparent

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(favBg)
                    .clickable(
                        interactionSource = favInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onFavoriteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AuraIcons.Favorite,
                    contentDescription = if (isFavorite) "In Favorites" else "Add to Favorites",
                    tint = favTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
