package com.rostomusic.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rostomusic.app.core.design.morphCorner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.rostomusic.app.core.design.RostoSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.BuildConfig
import com.rostomusic.app.R
import com.rostomusic.app.core.design.BentoCard
import com.rostomusic.app.core.design.Palette
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.RostoSlider
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.ScreenTitle
import com.rostomusic.app.core.design.SheetItem
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.repo.BackupManager
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.repo.LyricsRepository
import com.rostomusic.app.data.repo.ScanState
import com.rostomusic.app.data.settings.LyricsAlign
import com.rostomusic.app.data.settings.PlayerVisual
import com.rostomusic.app.data.settings.ProgressStyle
import com.rostomusic.app.data.settings.RowSubtitle
import com.rostomusic.app.data.settings.ThemeMode
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalBottomInset
import com.rostomusic.app.ui.LocalSettings
import com.rostomusic.app.ui.Routes
import com.rostomusic.app.ui.UiMessage
import com.rostomusic.app.ui.common.SheetTitle
import com.rostomusic.app.ui.equalizer.switchColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backup: BackupManager,
    private val library: LibraryRepository,
    private val lyrics: LyricsRepository,
) : ViewModel() {
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)

    fun export(uri: android.net.Uri) = viewModelScope.launch {
        messages.emit(UiMessage(if (backup.export(uri).isSuccess) R.string.msg_backup_done else R.string.msg_backup_failed))
    }

    fun restore(uri: android.net.Uri) = viewModelScope.launch {
        backup.restore(uri).fold(
            onSuccess = { messages.emit(UiMessage(R.string.msg_restore_done, listOf(it))) },
            onFailure = { messages.emit(UiMessage(R.string.msg_restore_failed)) },
        )
    }

    fun rescan() = viewModelScope.launch {
        library.rescan()
        lyrics.invalidateLrcIndex()
        messages.emit(UiMessage(R.string.msg_scan_done, listOf(library.songs.value.size)))
    }
}

private data class Choice<T>(val value: T, val label: Int)

@Composable
fun SettingsScreen(nav: NavController) {
    val app = LocalApp.current
    val s = LocalSettings.current
    val vm: SettingsViewModel = hiltViewModel()
    val c = Rosto.colors
    val context = LocalContext.current
    val scan by app.library.scanState.collectAsStateWithLifecycle()
    val folders by app.library.folders.collectAsStateWithLifecycle()
    val songs by app.library.songs.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<String?>(null) }
    var showAccent by rememberSaveable { mutableStateOf(false) }
    var showFolders by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var confirmOnline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.messages.collect { app.messages.emit(it) } }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.export(uri) }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.restore(uri) }
    val pickLrcTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            app.update { setLrcTreeUri(uri.toString()) }
            app.message(R.string.msg_lrc_folder_set)
        }
    }

    val themeChoices = listOf(Choice(ThemeMode.SYSTEM, R.string.theme_system), Choice(ThemeMode.LIGHT, R.string.theme_light), Choice(ThemeMode.DARK, R.string.theme_dark))
    val visualChoices = listOf(Choice(PlayerVisual.COVER, R.string.visual_cover), Choice(PlayerVisual.VINYL, R.string.visual_vinyl))
    val progressChoices = listOf(Choice(ProgressStyle.LINE, R.string.progress_line), Choice(ProgressStyle.WAVE, R.string.progress_wave))
    val subtitleChoices = listOf(
        Choice(RowSubtitle.ARTIST_DURATION, R.string.subtitle_artist_duration), Choice(RowSubtitle.ARTIST, R.string.subtitle_artist),
        Choice(RowSubtitle.ARTIST_ALBUM, R.string.subtitle_artist_album),
    )
    val alignChoices = listOf(Choice(LyricsAlign.START, R.string.align_start), Choice(LyricsAlign.CENTER, R.string.align_center))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen)
            .padding(top = Space.x3, bottom = LocalBottomInset.current + Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        ScreenTitle(stringResource(R.string.nav_settings))

        Group(stringResource(R.string.settings_appearance)) {
            ValueRow(R.drawable.ic_sun, stringResource(R.string.settings_theme), stringResource(themeChoices.first { it.value == s.themeMode }.label)) { sheet = "theme" }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SwitchRow(R.drawable.ic_image, stringResource(R.string.settings_dynamic_color), s.dynamicColor, stringResource(R.string.settings_dynamic_color_hint)) {
                    app.update { setDynamicColor(it) }
                }
            }
            if (!s.dynamicColor || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) Row(
                Modifier.fillMaxWidth().background(c.card).clickable(role = Role.Button) { showAccent = true }.heightIn(min = 64.dp).padding(horizontal = Space.x4, vertical = Space.x1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.rostomusic.app.core.design.IconPill(R.drawable.ic_palette)
                Spacer(Modifier.width(Space.x4))
                Text(stringResource(R.string.settings_accent), style = Rosto.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                Box(Modifier.size(24.dp).clip(CircleShape).background(c.accent))
            }
            ValueRow(R.drawable.ic_layout_dashboard, stringResource(R.string.customize_home), "") { nav.navigate(Routes.CUSTOMIZE) }
            ValueRow(R.drawable.ic_disc_3, stringResource(R.string.settings_player_visual), stringResource(visualChoices.first { it.value == s.playerVisual }.label)) { sheet = "visual" }
            ValueRow(R.drawable.ic_audio_lines, stringResource(R.string.settings_progress_style), stringResource(progressChoices.first { it.value == s.progressStyle }.label)) { sheet = "progress" }
            ValueRow(R.drawable.ic_list, stringResource(R.string.settings_row_subtitle), stringResource(subtitleChoices.first { it.value == s.rowSubtitle }.label)) { sheet = "subtitle" }
            SwitchRow(R.drawable.ic_clock, stringResource(R.string.settings_show_remaining), s.showRemaining) { app.update { setShowRemaining(it) } }
            SwitchRow(R.drawable.ic_vibrate, stringResource(R.string.settings_haptics), s.haptics) { app.update { setHaptics(it) } }
        }

        Group(stringResource(R.string.settings_library)) {
            val scanning = scan is ScanState.Scanning
            ValueRow(
                R.drawable.ic_refresh_cw, stringResource(R.string.rescan),
                if (scanning) stringResource(R.string.scanning) else pluralStringResource(R.plurals.songs_count, songs.size, songs.size),
            ) { if (!scanning) vm.rescan() }
            SliderRow(
                R.drawable.ic_timer, stringResource(R.string.settings_min_duration), s.minDurationSec.toFloat(), 0f..120f,
                stringResource(R.string.seconds_value, s.minDurationSec), steps = 11,
            ) { app.update { setMinDuration(it.roundToInt()) } }
            SwitchRow(
                R.drawable.ic_user_round, stringResource(R.string.settings_artist_info), s.artistInfoOnline,
                stringResource(R.string.settings_artist_info_hint),
            ) { app.update { setArtistInfoOnline(it) } }
            ValueRow(
                R.drawable.ic_folder_search, stringResource(R.string.settings_excluded_folders),
                if (s.excludedFolders.isEmpty()) stringResource(R.string.none) else s.excludedFolders.size.toString(),
            ) { showFolders = true }
        }

        Group(stringResource(R.string.settings_playback)) {
            SwitchRow(R.drawable.ic_circle_play, stringResource(R.string.settings_resume), s.resumeOnStart) { app.update { setResumeOnStart(it) } }
            SliderRow(
                R.drawable.ic_audio_lines, stringResource(R.string.settings_fade), s.fadeSec.toFloat(), 0f..12f,
                if (s.fadeSec == 0) stringResource(R.string.off) else stringResource(R.string.seconds_value, s.fadeSec), steps = 11,
            ) { app.update { setFadeSec(it.roundToInt()) } }
            SwitchRow(R.drawable.ic_volume_2, stringResource(R.string.settings_replay_gain), s.replayGain, stringResource(R.string.settings_replay_gain_hint)) { app.update { setReplayGain(it) } }
            SliderRow(
                R.drawable.ic_sliders_horizontal, stringResource(R.string.settings_preamp), s.preampDb, -12f..0f,
                "%+.0f dB".format(s.preampDb), steps = 11,
            ) { app.update { setPreampDb(it.roundToInt().toFloat()) } }
            ValueRow(R.drawable.ic_sliders_horizontal, stringResource(R.string.equalizer), stringResource(if (s.eqEnabled) R.string.on else R.string.off)) { nav.navigate(Routes.EQUALIZER) }
        }

        Group(stringResource(R.string.lyrics)) {
            SliderRow(
                R.drawable.ic_type, stringResource(R.string.settings_lyrics_size), s.lyricsScale, 0.8f..1.6f,
                "${(s.lyricsScale * 100).roundToInt()} %", steps = 7,
            ) { app.update { setLyricsScale((it * 10).roundToInt() / 10f) } }
            ValueRow(R.drawable.ic_align_left, stringResource(R.string.settings_lyrics_align), stringResource(alignChoices.first { it.value == s.lyricsAlign }.label)) { sheet = "align" }
            SwitchRow(
                R.drawable.ic_shield, stringResource(R.string.settings_explicit_lyrics), s.explicitLyrics,
                stringResource(if (s.explicitLyrics) R.string.settings_explicit_lyrics_on else R.string.settings_explicit_lyrics_off),
            ) { app.update { setExplicitLyrics(it) } }
            SwitchRow(R.drawable.ic_smartphone, stringResource(R.string.settings_keep_screen_on), s.keepScreenOn) { app.update { setKeepScreenOn(it) } }
            ValueRow(
                R.drawable.ic_folder_open, stringResource(R.string.settings_lrc_folder),
                stringResource(if (s.lrcTreeUri.isEmpty()) R.string.not_set else R.string.allowed), stringResource(R.string.settings_lrc_folder_hint),
            ) { pickLrcTree.launch(null) }
            SwitchRow(R.drawable.ic_globe, stringResource(R.string.settings_online_lyrics), s.onlineLyrics, stringResource(R.string.settings_online_lyrics_hint)) {
                if (it) confirmOnline = true else app.update { setOnlineLyrics(false) }
            }
        }

        Group(stringResource(R.string.settings_data)) {
            ValueRow(R.drawable.ic_chart_column, stringResource(R.string.stats), "") { nav.navigate(Routes.STATS) }
            ValueRow(R.drawable.ic_upload, stringResource(R.string.settings_backup), "", stringResource(R.string.settings_backup_hint)) { exportBackup.launch("rostomusic-backup.json") }
            ValueRow(R.drawable.ic_download, stringResource(R.string.settings_restore), "") { restoreBackup.launch(arrayOf("application/json", "*/*")) }
        }

        Group(stringResource(R.string.settings_about)) {
            ValueRow(R.drawable.ic_info, stringResource(R.string.app_name), stringResource(R.string.version_value, BuildConfig.VERSION_NAME)) { showAbout = true }
        }
    }

    when (sheet) {
        "theme" -> ChoiceSheet(stringResource(R.string.settings_theme), themeChoices, s.themeMode, { sheet = null }) { app.update { setThemeMode(it) } }
        "visual" -> ChoiceSheet(stringResource(R.string.settings_player_visual), visualChoices, s.playerVisual, { sheet = null }) { app.update { setPlayerVisual(it) } }
        "progress" -> ChoiceSheet(stringResource(R.string.settings_progress_style), progressChoices, s.progressStyle, { sheet = null }) { app.update { setProgressStyle(it) } }
        "subtitle" -> ChoiceSheet(stringResource(R.string.settings_row_subtitle), subtitleChoices, s.rowSubtitle, { sheet = null }) { app.update { setRowSubtitle(it) } }
        "align" -> ChoiceSheet(stringResource(R.string.settings_lyrics_align), alignChoices, s.lyricsAlign, { sheet = null }) { app.update { setLyricsAlign(it) } }
    }
    if (showAccent) AccentSheet(Color(s.accent), { showAccent = false }) { app.update { setAccent(it.toArgb()) } }
    if (showFolders) {
        val all = (folders.map { it.path } + s.excludedFolders).distinct().sorted()
        RostoSheet({ showFolders = false }) {
            SheetTitle(stringResource(R.string.settings_excluded_folders), stringResource(R.string.settings_excluded_folders_hint))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Space.x8)) {
                for (path in all) {
                    val excluded = path in s.excludedFolders
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Checkbox) {
                            app.update { setExcludedFolders(if (excluded) s.excludedFolders - path else s.excludedFolders + path) }
                        }.heightIn(min = 56.dp).padding(horizontal = Space.x6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RIcon(if (excluded) R.drawable.ic_eye_off else R.drawable.ic_folder, null, tint = if (excluded) c.danger else c.textSecondary, size = 20.dp)
                        Spacer(Modifier.width(Space.x4))
                        Column(Modifier.weight(1f)) {
                            Text(path.substringAfterLast('/'), style = Rosto.type.body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(path, style = Rosto.type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (excluded) Text(stringResource(R.string.excluded), style = Rosto.type.caption, color = c.danger)
                    }
                }
                SheetItem(R.drawable.ic_refresh_cw, stringResource(R.string.apply_and_rescan), { showFolders = false; vm.rescan() }, tint = c.accentText)
            }
        }
    }
    if (confirmOnline) {
        RostoAlert(
            title = stringResource(R.string.settings_online_lyrics),
            text = stringResource(R.string.online_lyrics_consent),
            confirmLabel = stringResource(R.string.enable),
            onDismiss = { confirmOnline = false },
            onConfirm = {
                confirmOnline = false
                app.update { setOnlineLyrics(true) }
            },
        )
    }
    if (showAbout) {
        RostoAlert(
            title = stringResource(R.string.app_name),
            text = stringResource(R.string.about_body, BuildConfig.VERSION_NAME),
            confirmLabel = stringResource(R.string.close),
            dismissLabel = null,
            onDismiss = { showAbout = false },
            onConfirm = { showAbout = false },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = Rosto.type.bodyStrong, color = Rosto.colors.textSecondary, modifier = Modifier.padding(start = Space.x4, top = Space.x2, bottom = Space.x2))
        com.rostomusic.app.core.design.RostoGroup(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun RowText(title: String, hint: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = Space.x2)) {
        Text(title, style = Rosto.type.bodyStrong, color = Rosto.colors.textPrimary)
        if (hint != null) Text(hint, style = Rosto.type.caption, color = Rosto.colors.textMuted)
    }
}

@Composable
private fun ValueRow(icon: Int, title: String, value: String, hint: String? = null, onClick: () -> Unit) {
    val c = Rosto.colors
    val press = remember { MutableInteractionSource() }
    val corner = morphCorner(press, rest = 4.dp, pressed = 16.dp)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(corner)).background(c.surfaceContainerLow).clickable(press, LocalIndication.current, role = Role.Button, onClick = onClick).heightIn(min = 68.dp).padding(horizontal = Space.x4, vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.rostomusic.app.core.design.IconPill(icon)
        Spacer(Modifier.width(Space.x4))
        RowText(title, hint, Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = Rosto.type.label, color = c.textMuted, maxLines = 1, modifier = Modifier.padding(start = Space.x2))
        Spacer(Modifier.width(Space.x1))
        RIcon(R.drawable.ic_chevron_right, null, tint = c.textMuted, size = 16.dp)
    }
}

@Composable
private fun SwitchRow(icon: Int, title: String, checked: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    val c = Rosto.colors
    // Règle 1 M3E : pas de clickable global sur la Row qui contient le Switch interactif
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(c.surfaceContainerLow).heightIn(min = 68.dp).padding(horizontal = Space.x4, vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.rostomusic.app.core.design.IconPill(icon)
        Spacer(Modifier.width(Space.x4))
        RowText(title, hint, Modifier.weight(1f))
        Spacer(Modifier.width(Space.x2))
        RostoSwitch(
            checked = checked,
            onCheckedChange = onChange,
            colors = switchColors(c),
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun SliderRow(
    icon: Int,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    val c = Rosto.colors
    var local by remember(value) { mutableFloatStateOf(value) }
    // Title on its own line and the value in a fixed slot, so the row never changes height.
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(c.surfaceContainerLow).padding(horizontal = Space.x4, vertical = Space.x3)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.rostomusic.app.core.design.IconPill(icon)
            Spacer(Modifier.width(Space.x4))
            Text(title, style = Rosto.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(start = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            RostoSlider(
                value = local, onValueChange = { local = it }, onValueChangeFinished = { onChange(local) },
                valueRange = range, steps = steps,
                stateDescriptionText = label,
                modifier = Modifier.weight(1f).semantics { contentDescription = title },
            )
            Text(label, style = Rosto.type.label, color = c.textMuted, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
        }
    }
}

@Composable
private fun <T> ChoiceSheet(title: String, choices: List<Choice<T>>, current: T, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    val c = Rosto.colors
    RostoSheet(onDismiss) {
        SheetTitle(title)
        Column(Modifier.padding(bottom = Space.x8)) {
            for (choice in choices) {
                val selected = choice.value == current
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onPick(choice.value); onDismiss() }.heightIn(min = 52.dp).padding(horizontal = Space.x6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(choice.label), style = if (selected) Rosto.type.bodyStrong else Rosto.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                    if (selected) RIcon(R.drawable.ic_check, null, tint = c.textPrimary, size = 20.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSheet(current: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val c = Rosto.colors
    var hue by remember { mutableFloatStateOf(run { val hsv = FloatArray(3); android.graphics.Color.colorToHSV(current.toArgb(), hsv); hsv[0] }) }
    val presetLabels = listOf(
        "Orange corail", "Vert émeraude", "Magenta", "Bleu azur",
        "Violet améthyste", "Turquoise", "Or ambré", "Rose carmin",
        "Indigo", "Citron vert", "Cyan", "Rose bonbon",
    )
    RostoSheet(onDismiss) {
        SheetTitle(stringResource(R.string.settings_accent), stringResource(R.string.settings_accent_hint))
        Column(Modifier.padding(horizontal = Space.x6).padding(bottom = Space.x8)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.x3),
                verticalArrangement = Arrangement.spacedBy(Space.x3),
            ) {
                Palette.AccentPresets.forEachIndexed { i, color ->
                    val name = presetLabels.getOrElse(i) { "Couleur ${i + 1}" }
                    val selected = color.toArgb() == current.toArgb()
                    Box(
                        Modifier
                            .size(Space.touch)
                            .clip(if (selected) RoundedCornerShape(16.dp) else CircleShape)
                            .background(color)
                            .then(if (selected) Modifier.border(3.dp, c.textPrimary, RoundedCornerShape(16.dp)) else Modifier)
                            .clickable(role = Role.RadioButton) { onPick(color) }
                            .semantics { contentDescription = name },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            RIcon(R.drawable.ic_check, null, tint = Palette.onColor(color), size = 20.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.x5))
            Text(stringResource(R.string.accent_custom), style = Rosto.type.label, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val custom = Color.hsv(hue, 0.78f, 0.92f)
                val label = stringResource(R.string.accent_custom)
                RostoSlider(
                    value = hue,
                    onValueChange = { hue = it },
                    onValueChangeFinished = { onPick(Color.hsv(hue, 0.78f, 0.92f)) },
                    valueRange = 0f..359f,
                    accent = custom,
                    stateDescriptionText = "${hue.roundToInt()} degrés",
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                )
                Spacer(Modifier.width(Space.x3))
                Box(Modifier.size(32.dp).clip(CircleShape).background(custom))
            }
        }
    }
}
