package com.rostomusic.app.ui.lyrics

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.PrimaryButton
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.TopBar
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.data.repo.LyricsSource
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.player.rememberPosition

@Composable
fun LyricsEditorScreen(nav: NavController) {
    val app = LocalApp.current
    val vm: LyricsEditorViewModel = hiltViewModel()
    val c = Rosto.colors
    val context = LocalContext.current
    val song by vm.song.collectAsStateWithLifecycle()
    val text by vm.text.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val source by vm.source.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val syncIndex by vm.syncIndex.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.events.collect { app.message(it) } }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { context.contentResolver.openInputStream(uri)?.use { vm.importBytes(it.readBytes()) } }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val ok = runCatching { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } }.isSuccess
            app.message(if (ok) R.string.msg_export_done else R.string.msg_export_failed)
        }
    }

    val s = song
    if (ready && s == null) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.edit_lyrics), onBack = { nav.popBackStack() })
            EmptyState(R.drawable.ic_circle_alert, stringResource(R.string.file_missing_title), stringResource(R.string.file_missing_body))
        }
        return
    }

    val lines = sync
    if (lines != null) {
        BackHandler { vm.finishSync(false) }
        val position by rememberPosition(vm.player, s?.id)
        val listState = rememberLazyListState()
        LaunchedEffect(syncIndex) { if (syncIndex > 1) listState.animateScrollToItem(syncIndex - 1) }
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TopBar(stringResource(R.string.lyrics_sync_title), onBack = { vm.finishSync(false) }) {
                Text(formatDuration(position), style = Rosto.type.label, color = c.textMuted, modifier = Modifier.padding(end = Space.x4))
            }
            Text(
                stringResource(R.string.lyrics_sync_hint), style = Rosto.type.label, color = c.textSecondary,
                modifier = Modifier.padding(horizontal = Space.screen),
            )
            LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                itemsIndexed(lines) { i, line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            line.timeMs?.let { LrcParser.formatTime(it) } ?: "--:--.--",
                            style = Rosto.type.caption, color = if (line.timeMs != null) c.accentText else c.textMuted,
                            modifier = Modifier.width(72.dp),
                        )
                        Text(
                            line.text, style = if (i == syncIndex) Rosto.type.heading else Rosto.type.body,
                            color = if (i == syncIndex) c.textPrimary else if (i < syncIndex) c.textSecondary else c.textMuted,
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = Space.screen, vertical = Space.x3), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x3), verticalAlignment = Alignment.CenterVertically) {
                    IconBtn(
                        if (state.isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled,
                        stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                        { vm.player.toggle() }, container = c.card,
                    )
                    IconBtn(R.drawable.ic_undo_2, stringResource(R.string.lyrics_sync_undo), { vm.undoMark() }, container = c.card, enabled = syncIndex > 0)
                    PrimaryButton(
                        stringResource(if (syncIndex >= lines.size) R.string.lyrics_sync_done else R.string.lyrics_sync_mark),
                        { if (syncIndex >= lines.size) vm.finishSync(true) else vm.mark() },
                        Modifier.weight(1f).height(64.dp),
                    )
                }
                if (syncIndex in 1 until lines.size) {
                    SecondaryButton(stringResource(R.string.lyrics_sync_finish_early), { vm.finishSync(true) }, Modifier.fillMaxWidth())
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.edit_lyrics), onBack = { nav.popBackStack() }) {
            if (source == LyricsSource.USER || source == LyricsSource.ONLINE) {
                IconBtn(R.drawable.ic_trash_2, stringResource(R.string.lyrics_delete_saved), { confirmDelete = true }, tint = c.textSecondary)
            }
        }
        if (s != null) {
            Text(
                "${s.title} · ${s.artist.ifEmpty { stringResource(R.string.unknown_artist) }}", style = Rosto.type.label, color = c.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Space.screen),
            )
        }
        LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            item { Pill(stringResource(R.string.lyrics_sync_start), false, { vm.startSync() }, icon = R.drawable.ic_timer) }
            item { Pill(stringResource(R.string.lyrics_import), false, { importer.launch(arrayOf("*/*")) }, icon = R.drawable.ic_file_down) }
            item { Pill(stringResource(R.string.lyrics_export), false, { exporter.launch("${s?.title ?: "lyrics"}.lrc") }, icon = R.drawable.ic_file_up) }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.x2)
                .clip(RoundedCornerShape(Radius.tile)).background(c.card).padding(Space.x4),
        ) {
            if (text.isEmpty()) Text(stringResource(R.string.lyrics_editor_placeholder), style = Rosto.type.body, color = c.textMuted)
            BasicTextField(
                value = text,
                onValueChange = { vm.text.value = it },
                textStyle = Rosto.type.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.accentText),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            stringResource(if (LrcParser.isSynced(text)) R.string.lyrics_format_synced else R.string.lyrics_format_plain),
            style = Rosto.type.caption, color = c.textMuted, modifier = Modifier.padding(horizontal = Space.screen),
        )
        Spacer(Modifier.height(Space.x2))
        PrimaryButton(
            stringResource(R.string.save), { vm.save { nav.popBackStack() } },
            Modifier.fillMaxWidth().padding(horizontal = Space.screen).padding(bottom = Space.x3), icon = R.drawable.ic_check,
        )
    }

    if (confirmDelete) {
        RostoAlert(
            title = stringResource(R.string.lyrics_delete_saved),
            text = stringResource(R.string.lyrics_delete_saved_body),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteSaved { nav.popBackStack() }
            },
        )
    }
}
