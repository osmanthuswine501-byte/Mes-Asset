package com.rostomusic.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.rostomusic.app.core.design.morphCorner
import com.rostomusic.app.core.design.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rostomusic.app.core.design.RostoSwitch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.ui.equalizer.switchColors

/**
 * The bar above the Albums and Morceaux lists: the sort criterion and its direction (opens the sort
 * sheet), shuffle, and, for albums, the list / grid switch. All pills are 44 dp, fully rounded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBar(label: String, descending: Boolean, onSort: () -> Unit, onShuffle: () -> Unit, grid: Boolean? = null, onGrid: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sortLabel = stringResource(R.string.sort_by_value, label)
        FilterChip(
            selected = false,
            onClick = onSort,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                RIcon(
                    id = R.drawable.ic_arrow_up,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (descending) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = null,
            modifier = Modifier.semantics { contentDescription = sortLabel },
        )
        Spacer(Modifier.weight(1f))
        val shuffleDesc = stringResource(R.string.shuffle)
        FilledTonalIconButton(
            onClick = onShuffle,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.semantics { contentDescription = shuffleDesc },
        ) {
            RIcon(
                id = R.drawable.ic_shuffle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 20.dp,
            )
        }
        if (grid != null) {
            Spacer(Modifier.width(Space.x2))
            val listDesc = stringResource(R.string.view_list)
            val gridDesc = stringResource(R.string.view_grid)
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = !grid,
                    onClick = { onGrid(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
                    modifier = Modifier.semantics { contentDescription = listDesc },
                ) {
                    RIcon(
                        id = R.drawable.ic_list,
                        contentDescription = null,
                        tint = if (!grid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp,
                    )
                }
                SegmentedButton(
                    selected = grid,
                    onClick = { onGrid(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
                    modifier = Modifier.semantics { contentDescription = gridDesc },
                ) {
                    RIcon(
                        id = R.drawable.ic_layout_grid,
                        contentDescription = null,
                        tint = if (grid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp,
                    )
                }
            }
        }
    }
}

/** "Trier par": direction, an on/off option, the criteria, and the genre filter. */
@Composable
fun SortSheet(
    section: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    descending: Boolean,
    onDescending: (Boolean) -> Unit,
    switchTitle: String,
    switchHint: String,
    switchOn: Boolean,
    onSwitch: (Boolean) -> Unit,
    genres: List<String>,
    genre: String?,
    onGenre: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Rosto.colors
    RostoSheet(onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = Space.x4).padding(bottom = Space.x4), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
            Row(Modifier.padding(horizontal = Space.x1), verticalAlignment = Alignment.CenterVertically) {
                // No kicker above the title: the sheet already sits on the list it sorts.
                @Suppress("UNUSED_VARIABLE") val unusedSection = section
                Text(stringResource(R.string.sort_by), style = Rosto.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
                Row(Modifier.height(44.dp).clip(CircleShape).background(c.background).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Direction(stringResource(R.string.sort_ascending), 0f, !descending) { onDescending(false) }
                    Direction(stringResource(R.string.sort_descending), 180f, descending) { onDescending(true) }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.background)
                    .padding(Space.x4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(switchTitle, style = Rosto.type.bodyStrong, color = c.textPrimary)
                    Text(switchHint, style = Rosto.type.label, color = c.textSecondary)
                }
                Spacer(Modifier.width(Space.x3))
                RostoSwitch(checked = switchOn, onCheckedChange = onSwitch, colors = switchColors(c))
            }
            if (genres.isNotEmpty()) {
                Text(stringResource(R.string.tab_genres), style = Rosto.type.label, color = c.textMuted, modifier = Modifier.padding(start = Space.x1))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.x2), contentPadding = PaddingValues(horizontal = Space.x1)) {
                    item { Pill(stringResource(R.string.genre_all), genre == null, { onGenre(null) }, container = c.background) }
                    items(genres) { g -> Pill(g, genre == g, { onGenre(g) }, container = c.background) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                options.forEachIndexed { i, label ->
                    val on = i == selected
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(Radius.cover))
                            .background(if (on) c.surfaceAlt else c.card)
                            .clickable(role = Role.RadioButton) { onSelect(i) }.padding(horizontal = Space.x4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = if (on) Rosto.type.bodyStrong else Rosto.type.body, color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (on) RIcon(R.drawable.ic_check, null, tint = c.textPrimary, size = 22.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Direction(label: String, rotation: Float, selected: Boolean, onClick: () -> Unit) {
    val c = Rosto.colors
    Row(
        Modifier.height(38.dp).clip(CircleShape).background(if (selected) c.surfaceAlt else c.background)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = Space.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x1),
    ) {
        RIcon(R.drawable.ic_arrow_up, null, Modifier.rotate(rotation), tint = c.textPrimary, size = 16.dp)
        Text(label, style = Rosto.type.label, color = c.textPrimary, maxLines = 1)
    }
}
