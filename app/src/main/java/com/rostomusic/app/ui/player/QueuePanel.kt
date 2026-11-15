package com.rostomusic.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.EqualizerBars
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.domain.QueueOps
import com.rostomusic.app.playback.PlayerState
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.common.TextInputDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class QueueRow(val key: String, val song: Song)

/** Rows need unique keys even when the same song is queued twice. */
private fun rowsOf(queue: List<Song>): List<QueueRow> {
    val seen = HashMap<Long, Int>()
    return queue.map { s ->
        val n = seen.getOrDefault(s.id, 0)
        seen[s.id] = n + 1
        QueueRow("${s.id}#$n", s)
    }
}

@Composable
fun QueuePanel(state: PlayerState) {
    val app = LocalApp.current
    val c = Rosto.colors
    var rows by remember(state.queue) { mutableStateOf(rowsOf(state.queue)) }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var saving by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        rows = QueueOps.move(rows, from.index, to.index)
    }
    val currentKey = rows.getOrNull(state.index)?.key
    LaunchedEffect(Unit) { if (state.index > 1) listState.scrollToItem(state.index - 1) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = Space.screen, end = Space.x2), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(R.plurals.songs_count, state.queue.size, state.queue.size),
                style = Rosto.type.label, color = c.textMuted, modifier = Modifier.weight(1f),
            )
            IconBtn(R.drawable.ic_save, stringResource(R.string.queue_save_as_playlist), { saving = true }, tint = c.textSecondary, iconSize = 20.dp)
            IconBtn(R.drawable.ic_trash_2, stringResource(R.string.queue_clear), { clearing = true }, tint = c.textSecondary, iconSize = 20.dp)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.x8)) {
            itemsIndexed(rows, key = { _, r -> r.key }) { index, row ->
                ReorderableItem(reorder, key = row.key) { dragging ->
                    val isCurrent = row.key == currentKey
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (dragging) c.card else c.background)
                            .then(if (isCurrent) Modifier.padding(horizontal = Space.x2).clip(androidx.compose.foundation.shape.RoundedCornerShape(com.rostomusic.app.core.design.Radius.image)).background(c.accent.copy(alpha = com.rostomusic.app.core.design.Alpha.NOW_PLAYING)) else Modifier)
                            .clickable { app.player.skipTo(index) }
                            .heightIn(min = 60.dp)
                            .padding(start = Space.x2, end = Space.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val handleDescription = stringResource(R.string.cd_reorder)
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(Space.touch)
                                .draggableHandle(
                                    onDragStarted = { dragFrom = index },
                                    onDragStopped = {
                                        val to = rows.indexOfFirst { it.key == row.key }
                                        if (dragFrom >= 0 && to >= 0 && to != dragFrom) app.player.move(dragFrom, to)
                                        dragFrom = -1
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) { RIcon(R.drawable.ic_grip_vertical, handleDescription, tint = c.textMuted, size = 18.dp) }
                        CoverArt(row.song, Modifier.size(com.rostomusic.app.core.design.CoverSize.s), Radius.small)
                        Spacer(Modifier.width(Space.x3))
                        Column(Modifier.weight(1f)) {
                            Text(
                                row.song.title, style = Rosto.type.bodyStrong, color = if (isCurrent) c.accentText else c.textPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                row.song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.label,
                                color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isCurrent) EqualizerBars(state.isPlaying, Modifier.padding(horizontal = Space.x3))
                        else IconBtn(R.drawable.ic_x, stringResource(R.string.queue_remove), { app.player.removeAt(index) }, tint = c.textMuted, iconSize = 18.dp)
                    }
                }
            }
        }
    }

    if (saving) {
        TextInputDialog(
            title = stringResource(R.string.queue_save_as_playlist),
            placeholder = stringResource(R.string.playlist_name),
            confirmLabel = stringResource(R.string.save),
            onDismiss = { saving = false },
            onConfirm = { name ->
                app.createPlaylist(name, state.queue.filter { it.id >= 0 })
                saving = false
            },
        )
    }
    if (clearing) {
        RostoAlert(
            title = stringResource(R.string.queue_clear),
            text = stringResource(R.string.queue_clear_body),
            confirmLabel = stringResource(R.string.queue_clear_confirm),
            destructive = true,
            onDismiss = { clearing = false },
            onConfirm = {
                clearing = false
                app.player.clearQueue()
            },
        )
    }
}
