package com.rostomusic.app.ui.info

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.domain.ReleaseInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.rostomusic.app.core.design.InfoChip
import com.rostomusic.app.core.design.DolbyBadge
import com.rostomusic.app.core.design.ToneScope
import com.rostomusic.app.core.design.rememberCoverTone
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.Badge
import com.rostomusic.app.core.design.BentoCard
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.PrimaryButton
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.TopBar
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.tags.AudioDetails
import com.rostomusic.app.data.tags.CoverChange
import com.rostomusic.app.data.tags.TagEditor
import com.rostomusic.app.data.tags.TagFields
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalBottomInset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SongInfoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository,
    private val tags: TagEditor,
) : ViewModel() {
    private val songId: Long = savedState.get<Long>("id") ?: -1

    val song: StateFlow<Song?> = library.songsById.map { it[songId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), library.song(songId))

    private val _details = MutableStateFlow<AudioDetails?>(null)
    val details: StateFlow<AudioDetails?> = _details

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    val events = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val s = library.song(songId) ?: return@launch
            _details.value = runCatching { tags.readDetails(s) }.getOrNull()
        }
    }

    fun writeRequest() = song.value?.let { runCatching { tags.writeRequest(listOf(it)) }.getOrNull() }

    fun save(fields: TagFields, cover: CoverChange, onDone: () -> Unit) {
        val s = song.value ?: return
        viewModelScope.launch {
            _saving.value = true
            val result = tags.write(s, fields, cover)
            if (result.isSuccess) {
                library.rescan(deep = false)
                reload()
                events.emit(R.string.msg_tags_saved)
                onDone()
            } else events.emit(R.string.msg_tags_failed)
            _saving.value = false
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongInfoScreen(nav: NavController) {
    val app = LocalApp.current
    val vm: SongInfoViewModel = hiltViewModel()
    val c = Rosto.colors
    val context = LocalContext.current
    val song by vm.song.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var fields by remember { mutableStateOf(TagFields()) }
    var cover by remember { mutableStateOf<CoverChange>(CoverChange.Keep) }
    LaunchedEffect(Unit) { vm.events.collect { app.message(it) } }

    val s = song
    if (s == null) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.song_info), onBack = { nav.popBackStack() })
            EmptyState(R.drawable.ic_circle_alert, stringResource(R.string.file_missing_title), stringResource(R.string.file_missing_body))
        }
        return
    }

    fun commit() = vm.save(fields, cover) {
        editing = false
        cover = CoverChange.Keep
    }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) commit() else app.message(R.string.msg_tags_denied)
    }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) commit() else app.message(R.string.msg_tags_denied)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null && bytes.size < 8_000_000) cover = CoverChange.Set(bytes, mime) else app.message(R.string.msg_cover_too_large)
            }
        }
    }
    fun requestSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sender = vm.writeRequest()
            if (sender != null) writeLauncher.launch(IntentSenderRequest.Builder(sender).build()) else app.message(R.string.msg_tags_failed)
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            commit()
        } else legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    // Read-only page: the cover at the top fades into its own colour, which covers the page.
    val tone = rememberCoverTone(if (editing) null else s.artUri, c.background)
    val toneOwner = remember { Any() }
    val leaving = com.rostomusic.app.core.design.LocalNavScope.current?.transition?.targetState == androidx.compose.animation.EnterExitState.PostExit
    LaunchedEffect(tone, editing, leaving) { if (editing || leaving) app.clearPageTone(toneOwner) else app.setPageTone(toneOwner, tone) }
    DisposableEffect(Unit) { onDispose { app.clearPageTone(toneOwner) } }
    ToneScope(if (editing) c.background else tone) {
    Column(Modifier.fillMaxSize().background(Rosto.colors.background).imePadding()) {
        TopBar(stringResource(if (editing) R.string.edit_tags else R.string.song_info), onBack = { if (editing) editing = false else nav.popBackStack() }) {
            if (!editing) {
                IconBtn(R.drawable.ic_pencil, stringResource(R.string.edit_tags), {
                    val d = details?.tags
                    fields = d ?: TagFields(title = s.title, artist = s.artist, album = s.album, albumArtist = s.albumArtist, genre = s.genre, composer = s.composer)
                    cover = CoverChange.Keep
                    editing = true
                })
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)
                .padding(bottom = LocalBottomInset.current + Space.x4),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            if (editing) {
                BentoCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(Space.x4), verticalAlignment = Alignment.CenterVertically) {
                        if (cover is CoverChange.Remove) CoverArt(-1, s.album.ifEmpty { s.title }, Modifier.size(com.rostomusic.app.core.design.CoverSize.l)) else CoverArt(s, Modifier.size(com.rostomusic.app.core.design.CoverSize.l))
                        Spacer(Modifier.width(Space.x4))
                        Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                            Text(
                                stringResource(
                                    when (cover) {
                                        is CoverChange.Set -> R.string.cover_will_change
                                        CoverChange.Remove -> R.string.cover_will_be_removed
                                        CoverChange.Keep -> R.string.cover_art
                                    }
                                ),
                                style = Rosto.type.label, color = c.textSecondary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                                SecondaryButton(stringResource(R.string.cover_choose), { coverPicker.launch("image/*") }, container = c.surfaceAlt)
                                IconBtn(R.drawable.ic_trash_2, stringResource(R.string.cover_remove), { cover = CoverChange.Remove }, tint = c.textSecondary)
                            }
                        }
                    }
                }
                BentoCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Space.x4), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                        Field(R.string.field_title, fields.title) { fields = fields.copy(title = it) }
                        Field(R.string.field_artist, fields.artist) { fields = fields.copy(artist = it) }
                        Field(R.string.field_album, fields.album) { fields = fields.copy(album = it) }
                        Field(R.string.field_album_artist, fields.albumArtist) { fields = fields.copy(albumArtist = it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                            Field(R.string.field_year, fields.year, Modifier.weight(1f), number = true) { fields = fields.copy(year = it) }
                            Field(R.string.field_track, fields.track, Modifier.weight(1f), number = true) { fields = fields.copy(track = it) }
                            Field(R.string.field_disc, fields.disc, Modifier.weight(1f), number = true) { fields = fields.copy(disc = it) }
                        }
                        Field(R.string.field_genre, fields.genre) { fields = fields.copy(genre = it) }
                        Field(R.string.field_composer, fields.composer) { fields = fields.copy(composer = it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                            Field(R.string.field_bpm, fields.bpm, Modifier.weight(1f), number = true) { fields = fields.copy(bpm = it) }
                            Spacer(Modifier.weight(2f))
                        }
                        Field(R.string.field_comment, fields.comment, singleLine = false) { fields = fields.copy(comment = it) }
                    }
                }
                Text(stringResource(R.string.edit_tags_note), style = Rosto.type.caption, color = c.textMuted)
                PrimaryButton(
                    stringResource(if (saving) R.string.saving else R.string.save_to_file), { requestSave() },
                    Modifier.fillMaxWidth(), icon = R.drawable.ic_save, enabled = !saving,
                )
                return@Column
            }

            InfoView(s, details)
        }
    }
    }
}

private fun dash(v: String?) = v?.trim().takeUnless { it.isNullOrEmpty() } ?: "—"

private fun khz(rate: Int): String {
    val k = rate / 1000f
    return if (k % 1f == 0f) k.toInt().toString() else "%.1f".format(k)
}

/** Read-only view: header, the four audio figures, then file, artwork, tags and listening cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoView(s: Song, d: AudioDetails?) {
    val c = Rosto.colors
    val context = LocalContext.current
    val dateTime = remember {
        { ms: Long ->
            if (ms <= 0) "—"
            else DateFormat.getMediumDateFormat(context).format(Date(ms)) + ", " + DateFormat.getTimeFormat(context).format(Date(ms))
        }
    }
    val t = d?.tags
    val year = t?.year?.ifEmpty { null } ?: s.year.takeIf { it > 0 }?.toString()
    val hiRes = d != null && s.isLossless && (d.sampleRate > 48_000 || d.bitsPerSample > 16)
    val dolby = ReleaseInfo.dolbyLabel(d?.codecMime)
    val isEp = ReleaseInfo.isEp(s.album, d?.releaseTypes.orEmpty())

    // Full-width cover fading into the page colour, then the name centred below it.
    Box(
        Modifier.layout { m, cons ->
            val extra = Space.screen.roundToPx() * 2
            val p = m.measure(cons.copy(minWidth = cons.maxWidth + extra, maxWidth = cons.maxWidth + extra))
            layout(cons.maxWidth, p.height) { p.place(-extra / 2, 0) }
        }.height(300.dp),
    ) {
        CoverArt(s, Modifier.fillMaxSize(), radius = 0.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 0.85f to c.background.copy(alpha = 0.85f), 1f to c.background)))
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(s.title, style = Rosto.type.title, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
        Text(
            listOfNotNull(s.artist.ifEmpty { stringResource(R.string.unknown_artist) }, year).joinToString(" · "),
            style = Rosto.type.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
    if (hiRes || s.isLossless || dolby != null || isEp) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.x2), verticalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            if (hiRes) QualityPill(stringResource(R.string.badge_hires_lossless), "${d?.bitsPerSample}-bit · ${khz(d?.sampleRate ?: 0)} kHz")
            else if (s.isLossless) InfoChip(stringResource(R.string.badge_lossless))
            dolby?.let { DolbyBadge(it) }
            if (isEp) InfoChip(stringResource(R.string.badge_ep))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
        StatTile(d?.codec?.ifEmpty { null } ?: s.format, "", stringResource(R.string.info_codec), Modifier.weight(1f), accent = true)
        StatTile(d?.sampleRate?.takeIf { it > 0 }?.let { khz(it) } ?: "—", "kHz", stringResource(R.string.info_frequency_short), Modifier.weight(1f))
        StatTile(d?.bitsPerSample?.takeIf { it > 0 }?.toString() ?: "—", "bit", stringResource(R.string.info_depth_short), Modifier.weight(1f))
        val ch = d?.channels ?: 0
        StatTile(
            ch.takeIf { it > 0 }?.toString() ?: "—", "ch",
            when (ch) {
                1 -> stringResource(R.string.channels_mono)
                2 -> stringResource(R.string.channels_stereo)
                0 -> stringResource(R.string.info_channels_short)
                else -> stringResource(R.string.channels_value, ch)
            },
            Modifier.weight(1f),
        )
    }

    val ct = d?.container
    InfoSection(stringResource(R.string.info_file)) {
        InfoLine(Cell(stringResource(R.string.info_file_name), s.path.substringAfterLast('/')))
        InfoLine(Cell(stringResource(R.string.info_path), s.path, mono = true))
        InfoLine(
            Cell(stringResource(R.string.info_size), Formatter.formatShortFileSize(context, s.size), mono = true),
            Cell(stringResource(R.string.info_bitrate), (d?.bitrateKbps?.takeIf { it > 0 } ?: (s.bitrate / 1000)).takeIf { it > 0 }?.let { "$it kb/s" }, mono = true),
        )
        InfoLine(
            Cell(stringResource(R.string.info_duration), formatDuration(s.duration), mono = true),
            Cell(stringResource(R.string.info_sample_rate), d?.sampleRate?.takeIf { it > 0 }?.let { stringResource(R.string.info_hz_value, it) }, mono = true),
        )
        InfoLine(
            Cell(stringResource(R.string.info_added), dateTime(s.dateAdded), mono = true),
            Cell(stringResource(R.string.info_modified), dateTime(s.dateModified), mono = true),
        )
        InfoLine(Cell(stringResource(R.string.info_container), ct?.name), Cell(stringResource(R.string.info_profile), ct?.profile))
        if (ct != null && ct.majorBrand.isNotEmpty()) {
            InfoLine(
                Cell(stringResource(R.string.info_major_brand), ct.majorBrand, mono = true),
                Cell(stringResource(R.string.info_minor_version), ct.minorVersion, mono = true),
            )
            InfoLine(Cell(stringResource(R.string.info_compatible_brands), ct.compatibleBrands.joinToString(", "), mono = true))
        }
    }

    InfoSection(stringResource(R.string.info_artwork)) {
        val art = d?.artwork
        if (art == null) {
            InfoLine(Cell("", stringResource(R.string.info_no_artwork)))
        } else {
            InfoLine(
                Cell(stringResource(R.string.info_dimensions), if (art.width > 0) "${art.width} × ${art.height}" else null, mono = true),
                Cell(stringResource(R.string.info_format), art.format, mono = true),
                Cell(stringResource(R.string.info_size), Formatter.formatShortFileSize(context, art.bytes.toLong()), mono = true),
            )
        }
    }

    InfoSection(stringResource(R.string.info_tags)) {
        InfoLine(Cell(stringResource(R.string.field_title), t?.title?.ifEmpty { null } ?: s.title))
        InfoLine(Cell(stringResource(R.string.field_album), t?.album?.ifEmpty { null } ?: s.album))
        InfoLine(
            Cell(stringResource(R.string.field_artist), t?.artist?.ifEmpty { null } ?: s.artist),
            Cell(stringResource(R.string.field_album_artist), t?.albumArtist?.ifEmpty { null } ?: s.albumArtist),
        )
        InfoLine(
            Cell(stringResource(R.string.field_genre), t?.genre?.ifEmpty { null } ?: s.genre),
            Cell(stringResource(R.string.field_composer), t?.composer?.ifEmpty { null } ?: s.composer),
        )
        InfoLine(
            Cell(stringResource(R.string.field_year), year, mono = true),
            Cell(stringResource(R.string.field_track), t?.track?.ifEmpty { null } ?: s.track.takeIf { it > 0 }?.toString(), mono = true),
            Cell(stringResource(R.string.field_disc), t?.disc?.ifEmpty { null } ?: s.disc.takeIf { it > 0 }?.toString(), mono = true),
        )
        InfoLine(
            Cell(stringResource(R.string.field_bpm), t?.bpm, mono = true),
            Cell(stringResource(R.string.info_replay_gain), d?.replayGainDb?.let { "%+.2f dB".format(it) }, mono = true),
        )
        InfoLine(Cell(stringResource(R.string.field_comment), t?.comment))
        InfoLine(Cell(stringResource(R.string.info_encoder), d?.encoder))
        d?.extraTags?.forEach { (label, value) -> InfoLine(Cell(label, value)) }
    }

    InfoSection(stringResource(R.string.info_listening)) {
        InfoLine(
            Cell(stringResource(R.string.info_play_count), pluralStringResource(R.plurals.plays_count, s.playCount, s.playCount)),
            Cell(stringResource(R.string.info_last_played), if (s.lastPlayed > 0) dateTime(s.lastPlayed) else stringResource(R.string.never)),
        )
        InfoLine(Cell(stringResource(R.string.info_listened), formatDuration(s.listenedMs), mono = true))
    }
}

private class Cell(val label: String, val value: String?, val mono: Boolean = false)

@Composable
private fun QualityPill(label: String, detail: String) {
    val c = Rosto.colors
    Row(
        Modifier.clip(RoundedCornerShape(Radius.small)).background(c.surfaceAlt).padding(horizontal = Space.x3, vertical = Space.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x2),
    ) {
        RIcon(R.drawable.ic_gem, null, tint = c.textPrimary, size = 16.dp)
        Text(label, style = Rosto.type.bodyStrong, color = c.textPrimary)
        Text(detail, style = Rosto.type.label.copy(fontFamily = FontFamily.Monospace), color = c.textSecondary)
    }
}

@Composable
private fun StatTile(value: String, unit: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val c = Rosto.colors
    BentoCard(modifier) {
        Column(Modifier.padding(horizontal = Space.x3, vertical = Space.x4)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value, style = Rosto.type.title, color = c.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (unit.isNotEmpty() && value != "—") {
                    Spacer(Modifier.width(2.dp))
                    Text(unit, style = Rosto.type.caption.copy(fontFamily = FontFamily.Monospace), color = c.textMuted, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Spacer(Modifier.height(Space.x1))
            Text(label, style = Rosto.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Rosto.colors
    Column {
        Text(title, style = Rosto.type.heading, color = c.textPrimary, modifier = Modifier.padding(start = Space.x1, top = Space.x2, bottom = Space.x2))
        BentoCard(Modifier.fillMaxWidth()) { Column(content = content) }
    }
}

/** One row of the card: one to three cells side by side, separated by thin lines. */
@Composable
private fun ColumnScope.InfoLine(vararg cells: Cell) {
    val c = Rosto.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        cells.forEachIndexed { i, cell ->
            if (i > 0) Box(Modifier.padding(vertical = Space.x3).width(1.dp).fillMaxHeight().background(c.hairline))
            Column(Modifier.weight(1f).padding(horizontal = Space.x4, vertical = Space.x3)) {
                if (cell.label.isNotEmpty()) {
                    Text(cell.label, style = Rosto.type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(Space.x1))
                }
                Text(
                    dash(cell.value),
                    style = if (cell.mono) androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = if (cell.value.isNullOrBlank()) c.textMuted else c.textPrimary,
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}

@Composable
private fun Field(
    label: Int,
    value: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    val c = Rosto.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label), style = Rosto.type.label) },
        singleLine = singleLine,
        textStyle = Rosto.type.body,
        shape = RoundedCornerShape(Radius.cover),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accentText, unfocusedBorderColor = c.hairline, cursorColor = c.accentText,
            focusedLabelColor = c.accentText, unfocusedLabelColor = c.textMuted,
            focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
