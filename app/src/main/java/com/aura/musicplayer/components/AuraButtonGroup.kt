package com.aura.musicplayer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.theme.AuraCornerRadius

/**
 * Official Material 3 Expressive Connected Button Group (m3.material.io/components/button-groups/specs).
 * Specifications:
 * - Inner spacing: strictly 2dp between buttons.
 * - Corner radii: Outer edges fully rounded (pill), inner corners 4dp to 8dp.
 * - Selected item morphs shape and expands with spring physics.
 */
@Composable
fun <T> AuraConnectedButtonGroup(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    Surface(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(AuraCornerRadius.Full)),
        shape = RoundedCornerShape(AuraCornerRadius.Full),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = item == selectedItem
                val isFirst = index == 0
                val isLast = index == items.size - 1

                val startRadius by animateDpAsState(
                    targetValue = if (isFirst) AuraCornerRadius.Full else (if (isSelected) 12.dp else 4.dp),
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
                    label = "StartRadius"
                )
                val endRadius by animateDpAsState(
                    targetValue = if (isLast) AuraCornerRadius.Full else (if (isSelected) 12.dp else 4.dp),
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
                    label = "EndRadius"
                )

                val itemShape = RoundedCornerShape(
                    topStart = startRadius,
                    bottomStart = startRadius,
                    topEnd = endRadius,
                    bottomEnd = endRadius
                )

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    animationSpec = spring(dampingRatio = 1.0f, stiffness = 600f),
                    label = "ItemBgColor"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(dampingRatio = 1.0f, stiffness = 600f),
                    label = "ItemTextColor"
                )

                Box(
                    modifier = Modifier
                        .height(height - 4.dp)
                        .clip(itemShape)
                        .background(backgroundColor)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onItemSelected(item) }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }

                if (!isLast) {
                    // Exact 2dp spacing token between buttons
                    Box(modifier = Modifier.padding(horizontal = 1.dp))
                }
            }
        }
    }
}
