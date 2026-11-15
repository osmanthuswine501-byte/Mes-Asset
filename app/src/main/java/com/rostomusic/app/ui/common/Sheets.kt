package com.rostomusic.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.RostoSlider
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.SheetItem
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.CollectionType
import com.rostomusic.app.playback.SleepState
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.Routes
import com.rostomusic.app.ui.SongMenu
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SheetTitle(text: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.x6).padding(bottom = Space.x3)) {
        Text(text, style = Rosto.type.heading, color = Rosto.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, style = Rosto.type.label, color = Rosto.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SongMenuSheet(menu: SongMenu, nav: NavController, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val song = menu.song
    val live = app.library.songsById.collectAsState().value[song.id] ?: song
    fun close(action: () -> Unit) {
        onDismiss()
        action()
    }
    RostoSheet(onDismiss) {
        Row(Modifier.padding(horizontal = Space.x6).padding(bottom = Space.x4), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(song, Modifier.size(com.rostomusic.app.core.design.CoverSize.m), Radius.small)
            Spacer(Modifier.width(Space.x4))
            Column {
                Text(song.title, style = Rosto.type.heading, color = Rosto.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.label,
                    color = Rosto.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
            SheetItem(R.drawable.ic_corner_down_right, stringResource(R.string.play_next), { close { app.playNext(listOf(song)) } })
            SheetItem(R.drawable.ic_list_end, stringResource(R.string.add_to_queue), { close { app.enqueue(listOf(song)) } })
            SheetItem(R.drawable.ic_list_plus, stringResource(R.string.add_to_playlist), { close { app.playlistPicker.value = listOf(song) } })
            SheetItem(
                if (live.favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                stringResource(if (live.favorite) R.string.remove_favorite else R.string.add_favorite),
                { close { app.toggleFavorite(live) } },
            )
            if (song.albumId >= 0) {
                SheetItem(R.drawable.ic_disc_3, stringResource(R.string.go_to_album), {
                    close { app.collapsePlayer(); nav.navigate(Routes.collection(CollectionType.ALBUM, song.albumId.toString())) }
                })
                SheetItem(R.drawable.ic_user_round, stringResource(R.string.go_to_artist), {
                    close { app.collapsePlayer(); nav.navigate(Routes.collection(CollectionType.ARTIST, song.artist)) }
                })
                SheetItem(R.drawable.ic_captions, stringResource(R.string.edit_lyrics), {
                    close { app.collapsePlayer(); nav.navigate(Routes.lyricsEditor(song.id)) }
                })
                SheetItem(R.drawable.ic_info, stringResource(R.string.song_info), {
                    close { app.collapsePlayer(); nav.navigate(Routes.info(song.id)) }
                })
                SheetItem(R.drawable.ic_share_2, stringResource(R.string.share), { close { app.share(context, song) } })
            }
            if (menu.playlistEntryId != null) {
                SheetItem(
                    R.drawable.ic_minus, stringResource(R.string.remove_from_playlist),
                    { close { app.removePlaylistEntry(menu.playlistEntryId) } }, tint = Rosto.colors.danger,
                )
            }
        }
    }
}

@Composable
fun PlaylistPickerSheet(songs: List<Song>, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val playlists by app.library.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    RostoSheet(onDismiss) {
        SheetTitle(stringResource(R.string.add_to_playlist), pluralStringResource(R.plurals.songs_count, songs.size, songs.size))
        LazyColumn(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
            item { SheetItem(R.drawable.ic_plus, stringResource(R.string.new_playlist), { creating = true }) }
            items(playlists, key = { it.id }) { p ->
                SheetItem(
                    R.drawable.ic_list_music, p.name,
                    { app.addToPlaylist(p.id, songs); onDismiss() },
                    trailing = p.entries.size.toString(),
                )
            }
        }
    }
    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.new_playlist),
            placeholder = stringResource(R.string.playlist_name),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { creating = false },
            onConfirm = { name ->
                app.createPlaylist(name, songs)
                creating = false
                onDismiss()
            },
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = "",
    singleLine: Boolean = true,
) {
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val c = Rosto.colors
    RostoAlert(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = { if (value.isNotBlank()) onConfirm(value.trim()) },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = singleLine,
            placeholder = { Text(placeholder, style = Rosto.type.body, color = c.textMuted) },
            textStyle = Rosto.type.body,
            shape = RoundedCornerShape(Radius.cover),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onConfirm(value.trim()) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accentText, unfocusedBorderColor = c.hairline, cursorColor = c.accentText,
                focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
fun SleepTimerSheet(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val state by app.sleepTimer.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state is SleepState.Until) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val status = when (val s = state) {
        SleepState.Off -> stringResource(R.string.sleep_off)
        SleepState.EndOfTrack -> stringResource(R.string.sleep_end_of_track_active)
        is SleepState.Until -> stringResource(R.string.sleep_remaining, formatDuration((s.endAt - now).coerceAtLeast(0)))
    }
    RostoSheet(onDismiss) {
        SheetTitle(stringResource(R.string.sleep_timer), status)
        Column(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
            for (minutes in listOf(5, 15, 30, 45, 60, 90)) {
                SheetItem(R.drawable.ic_timer, stringResource(R.string.minutes_value, minutes), {
                    app.sleepTimer.start(minutes)
                    app.message(R.string.msg_sleep_set, minutes)
                    onDismiss()
                })
            }
            SheetItem(R.drawable.ic_music, stringResource(R.string.sleep_end_of_track), {
                app.sleepTimer.endOfTrack()
                onDismiss()
            })
            if (state != SleepState.Off) {
                SheetItem(R.drawable.ic_x, stringResource(R.string.sleep_cancel), {
                    app.sleepTimer.cancel()
                    onDismiss()
                }, tint = Rosto.colors.danger)
            }
        }
    }
}

@Composable
fun SpeedSheet(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val state by app.playerState.collectAsState()
    var value by remember { mutableFloatStateOf(state.speed) }
    val c = Rosto.colors
    RostoSheet(onDismiss) {
        SheetTitle(stringResource(R.string.playback_speed), "%.2f×".format(value))
        Column(Modifier.navigationBarsPadding().padding(horizontal = Space.x6).padding(bottom = Space.x6)) {
            RostoSlider(
                value = value,
                onValueChange = { value = (it * 20).roundToInt() / 20f },
                onValueChangeFinished = { app.player.setSpeed(value) },
                valueRange = 0.5f..2f,
                stateDescriptionText = "%.2f×".format(value),
            )
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                for (preset in listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) {
                    Pill(
                        text = "${if (preset % 1f == 0f) preset.toInt().toString() else preset.toString()}×",
                        selected = value == preset,
                        onClick = {
                            value = preset
                            app.player.setSpeed(preset)
                        },
                        container = c.surfaceAlt,
                    )
                }
            }
        }
    }
}
