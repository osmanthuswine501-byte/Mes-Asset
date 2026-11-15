package com.rostomusic.app.ui.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rostomusic.app.core.design.RostoSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.TopBar
import com.rostomusic.app.data.repo.CollectionType
import com.rostomusic.app.domain.HomeConfig
import com.rostomusic.app.domain.HomeLayout
import com.rostomusic.app.domain.SectionId
import com.rostomusic.app.domain.ShortcutId
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.equalizer.switchColors
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

fun sectionLabel(id: SectionId) = when (id) {
    SectionId.HERO -> R.string.section_hero
    SectionId.PINNED -> R.string.section_pinned
    SectionId.SHORTCUTS -> R.string.section_shortcuts
    SectionId.DAILY_MIX -> R.string.daily_mix
    SectionId.DISCOVER -> R.string.section_discover
    SectionId.ARTIST_SPOTLIGHT -> R.string.section_spotlight
    SectionId.STATS -> R.string.section_stats
    SectionId.RECENT_PLAYED -> R.string.section_recently_played
    SectionId.RECENT_ADDED -> R.string.section_recently_added
    SectionId.MOST_PLAYED -> R.string.section_most_played
    SectionId.PLAYLISTS -> R.string.tab_playlists
    SectionId.ARTISTS -> R.string.section_your_artists
    SectionId.ALBUM_WALL -> R.string.section_album_wall
}

fun presetLabel(name: String) = when (name) {
    "pinned" -> R.string.preset_pinned
    "minimal" -> R.string.preset_minimal
    "mix" -> R.string.preset_mix
    "artists" -> R.string.preset_artists
    "discover" -> R.string.preset_discover
    "wall" -> R.string.preset_wall
    "stats" -> R.string.preset_stats
    "shortcuts" -> R.string.preset_shortcuts
    "compact" -> R.string.preset_compact
    else -> R.string.preset_default
}

@Composable
fun CustomizeScreen(nav: NavController) {
    val app = LocalApp.current
    val vm: HomeViewModel = hiltViewModel()
    val c = Rosto.colors
    val scope = rememberCoroutineScope()
    val saved by vm.layout.collectAsStateWithLifecycle()
    // Local copy so dragging stays smooth; written back after each drop.
    var layout by remember(saved) { mutableStateOf(saved) }
    var styleFor by remember { mutableStateOf<SectionId?>(null) }
    var editShortcuts by remember { mutableStateOf(false) }
    var editPins by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    fun save(change: (HomeLayout) -> HomeLayout) {
        layout = change(layout)
        scope.launch { app.settingsStore.updateHome(change) }
    }

    val listState = rememberLazyListState()
    // Two header items sit above the sections.
    val offset = 2
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = from.index - offset
        val t = to.index - offset
        if (f in layout.sections.indices && t in layout.sections.indices) layout = HomeConfig.move(layout, f, t)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.customize_home), onBack = { nav.popBackStack() })
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.x8)) {
            item(key = "presets") {
                Text(
                    stringResource(R.string.customize_presets), style = Rosto.type.label, color = c.textMuted,
                    modifier = Modifier.padding(start = Space.screen, top = Space.x2, bottom = Space.x1),
                )
                LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    items(HomeConfig.PRESETS) { name ->
                        Pill(stringResource(presetLabel(name)), layout.preset == name, { save { HomeConfig.applyPreset(it, name) } })
                    }
                }
            }
            item(key = "sections_title") {
                Row(Modifier.padding(start = Space.screen, end = Space.screen, top = Space.x5, bottom = Space.x1)) {
                    Text(stringResource(R.string.customize_sections), style = Rosto.type.label, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.customize_drag_hint), style = Rosto.type.caption, color = c.textMuted)
                }
            }
            itemsIndexed(layout.sections, key = { _, s -> s.id.name }) { _, section ->
                ReorderableItem(reorder, key = section.id.name) { dragging ->
                    val styles = HomeConfig.stylesFor(section.id)
                    val sub = when (section.id) {
                        SectionId.SHORTCUTS -> stylesLabel(section.style) + " · " + pluralStringResource(R.plurals.shortcuts_count, layout.shortcuts.size, layout.shortcuts.size)
                        SectionId.PINNED -> pluralStringResource(R.plurals.pins_count, layout.pins.size, layout.pins.size)
                        else -> stylesLabel(section.style)
                    }
                    val editable = styles.size > 1 || section.id == SectionId.PINNED
                    Row(
                        Modifier.padding(horizontal = Space.x3, vertical = 3.dp).fillMaxWidth().clip(RoundedCornerShape(Radius.image))
                            .background(if (dragging) c.surfaceAlt else c.card)
                            .clickable(enabled = editable, role = Role.Button) {
                                if (section.id == SectionId.PINNED) editPins = true else styleFor = section.id
                            }
                            .heightIn(min = 64.dp).padding(end = Space.x3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(Space.touch).draggableHandle(onDragStopped = { val l = layout; scope.launch { app.settingsStore.updateHome { l.copy(preset = it.preset) } } }),
                            contentAlignment = Alignment.Center,
                        ) { RIcon(R.drawable.ic_grip_vertical, stringResource(R.string.cd_reorder), tint = c.textMuted, size = 18.dp) }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(sectionLabel(section.id)), style = Rosto.type.bodyStrong,
                                color = if (section.visible) c.textPrimary else c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sub, style = Rosto.type.caption, color = c.textSecondary, maxLines = 1)
                                if (editable) {
                                    Spacer(Modifier.width(2.dp))
                                    RIcon(R.drawable.ic_chevron_right, null, tint = c.textMuted, size = 12.dp)
                                }
                            }
                        }
                        if (section.id == SectionId.SHORTCUTS) {
                            IconBtn(R.drawable.ic_pencil, stringResource(R.string.customize_shortcuts), { editShortcuts = true }, tint = c.textSecondary, iconSize = 18.dp)
                        }
                        RostoSwitch(
                            checked = section.visible, colors = switchColors(c),
                            onCheckedChange = { v -> save { HomeConfig.setVisible(it, section.id, v) } },
                        )
                    }
                }
            }
            item(key = "reset") {
                SecondaryButton(
                    stringResource(R.string.customize_reset), { confirmReset = true },
                    Modifier.padding(horizontal = Space.screen, vertical = Space.x5).fillMaxWidth(), icon = R.drawable.ic_rotate_ccw,
                )
            }
        }
    }

    styleFor?.let { id ->
        RostoSheet({ styleFor = null }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = Space.x4)) {
                Text(
                    stringResource(sectionLabel(id)), style = Rosto.type.heading, color = c.textPrimary,
                    modifier = Modifier.padding(horizontal = Space.x6, vertical = Space.x2),
                )
                val current = layout.section(id)?.style
                HomeConfig.stylesFor(id).forEach { style ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.RadioButton) {
                            save { HomeConfig.setStyle(it, id, style) }
                            styleFor = null
                        }.heightIn(min = 52.dp).padding(horizontal = Space.x6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stylesLabel(style), style = Rosto.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                        if (style == current) RIcon(R.drawable.ic_check, null, tint = c.textPrimary, size = 20.dp)
                    }
                }
            }
        }
    }

    if (editShortcuts) {
        RostoSheet({ editShortcuts = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = Space.x4)) {
                Row(Modifier.padding(horizontal = Space.x6, vertical = Space.x2), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.customize_shortcuts), style = Rosto.type.heading, color = c.textPrimary, modifier = Modifier.weight(1f))
                    Text("${layout.shortcuts.size}/9", style = Rosto.type.label, color = c.textMuted)
                }
                Text(
                    stringResource(R.string.customize_shortcuts_hint), style = Rosto.type.caption, color = c.textMuted,
                    modifier = Modifier.padding(horizontal = Space.x6).padding(bottom = Space.x2),
                )
                ShortcutId.entries.forEach { id ->
                    val on = id in layout.shortcuts
                    val position = layout.shortcuts.indexOf(id)
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = on || layout.shortcuts.size < 9, role = Role.Checkbox) {
                            save { HomeConfig.toggleShortcut(it, id) }
                        }.heightIn(min = 52.dp).padding(horizontal = Space.x6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(shortcutLabel(id)), style = Rosto.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(if (on) c.textPrimary else c.surfaceAlt),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) Text((position + 1).toString(), style = Rosto.type.caption, color = c.background)
                        }
                    }
                }
            }
        }
    }

    if (editPins) {
        PinsSheet(layout, onUnpin = { pin -> save { HomeConfig.togglePin(it, pin) } }, onDismiss = { editPins = false })
    }

    if (confirmReset) {
        RostoAlert(
            title = stringResource(R.string.customize_reset),
            text = stringResource(R.string.customize_reset_body),
            confirmLabel = stringResource(R.string.reset),
            onConfirm = {
                save { HomeConfig.applyPreset(it, HomeConfig.DEFAULT_PRESET) }
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun PinsSheet(layout: HomeLayout, onUnpin: (String) -> Unit, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val playlists by app.library.playlists.collectAsStateWithLifecycle()
    RostoSheet(onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Space.x4)) {
            Text(
                stringResource(R.string.section_pinned), style = Rosto.type.heading, color = c.textPrimary,
                modifier = Modifier.padding(horizontal = Space.x6, vertical = Space.x2),
            )
            if (layout.pins.isEmpty()) {
                Text(
                    stringResource(R.string.pinned_empty), style = Rosto.type.body, color = c.textSecondary,
                    modifier = Modifier.padding(horizontal = Space.x6, vertical = Space.x2),
                )
            }
            layout.pins.forEach { pin ->
                val type = pin.substringBefore(':')
                val key = pin.substringAfter(':', "")
                val list = remember(pin, songs, playlists) { app.library.collectionNow(type, key) }
                val title = when (type) {
                    CollectionType.ALBUM -> list.firstOrNull()?.album ?: key
                    CollectionType.PLAYLIST -> playlists.firstOrNull { it.id == key.toLongOrNull() }?.name ?: key
                    CollectionType.FOLDER -> key.substringAfterLast('/')
                    else -> key.ifEmpty { type }
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = Space.x6, end = Space.x3), verticalAlignment = Alignment.CenterVertically) {
                    CoverArt(list.firstOrNull()?.albumId ?: -1, title, Modifier.size(com.rostomusic.app.core.design.CoverSize.s), if (type == CollectionType.ARTIST) 400.dp else Radius.small)
                    Spacer(Modifier.width(Space.x3))
                    Text(title, style = Rosto.type.body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconBtn(R.drawable.ic_pin_off, stringResource(R.string.unpin_from_home), { onUnpin(pin) }, tint = c.textSecondary, iconSize = 18.dp)
                }
            }
            Spacer(Modifier.height(Space.x2))
            Text(
                stringResource(R.string.customize_pins_hint), style = Rosto.type.caption, color = c.textMuted,
                modifier = Modifier.padding(horizontal = Space.x6),
            )
        }
    }
}
