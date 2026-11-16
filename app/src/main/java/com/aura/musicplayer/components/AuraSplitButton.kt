package com.aura.musicplayer.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.musicplayer.theme.AuraCornerRadius

/**
 * Official Material 3 Expressive Split Button (m3.material.io/components/split-button/specs).
 * Specifications:
 * - 2dp between space token.
 * - Leading action button: outer corner full pill, inner corner 4dp.
 * - Trailing menu button: inner corner 4dp, outer corner full pill.
 * - Inner corners animate to 8dp upon press/expansion.
 */
@Composable
fun <T> AuraSplitButton(
    selectedOption: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val leadingInteraction = remember { MutableInteractionSource() }
    val isLeadingPressed by leadingInteraction.collectIsPressedAsState()

    val trailingInteraction = remember { MutableInteractionSource() }
    val isTrailingPressed by trailingInteraction.collectIsPressedAsState()

    val leadingInnerRadius by animateDpAsState(
        targetValue = if (isLeadingPressed) 8.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "LeadingInnerRadius"
    )

    val trailingInnerRadius by animateDpAsState(
        targetValue = if (isTrailingPressed || isMenuExpanded) 8.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "TrailingInnerRadius"
    )

    val leadingShape = RoundedCornerShape(
        topStart = AuraCornerRadius.Full,
        bottomStart = AuraCornerRadius.Full,
        topEnd = leadingInnerRadius,
        bottomEnd = leadingInnerRadius
    )

    val trailingShape = RoundedCornerShape(
        topStart = trailingInnerRadius,
        bottomStart = trailingInnerRadius,
        topEnd = AuraCornerRadius.Full,
        bottomEnd = AuraCornerRadius.Full
    )

    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Leading Primary Action Button
        Box(
            modifier = Modifier
                .height(height)
                .clip(leadingShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    interactionSource = leadingInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = { isMenuExpanded = true }
                )
                .padding(start = 14.dp, end = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = labelProvider(selectedOption),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Exact 2dp between space token
        Spacer(modifier = Modifier.width(2.dp))

        // 2. Trailing Menu Trigger Button
        Box(
            modifier = Modifier
                .height(height)
                .clip(trailingShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    interactionSource = trailingInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = { isMenuExpanded = !isMenuExpanded }
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AuraIcons.ArrowDropDown,
                contentDescription = "Select options",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = labelProvider(option),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
