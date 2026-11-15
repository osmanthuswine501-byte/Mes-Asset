package com.rostomusic.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp as floatLerp
import kotlin.math.absoluteValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.core.snap
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.rostomusic.app.core.design.DolbyBadge
import com.rostomusic.app.core.design.ToneScope
import com.rostomusic.app.core.design.rememberCoverTone
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.AccentScope
import com.rostomusic.app.core.design.Badge
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.LocalReduceMotion
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.SheetItem
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.core.design.rememberHaptic
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.settings.PlayerVisual
import com.rostomusic.app.data.settings.ProgressStyle
import com.rostomusic.app.domain.QueueOps
import com.rostomusic.app.domain.ReleaseInfo
import com.rostomusic.app.playback.OutputSwitcher
import com.rostomusic.app.playback.PlayerState
import com.rostomusic.app.playback.SleepState
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalSettings
import com.rostomusic.app.ui.PlayerPanel
import com.rostomusic.app.ui.Routes
import com.rostomusic.app.ui.SongMenu
import com.rostomusic.app.ui.common.SheetTitle
import com.rostomusic.app.ui.common.SleepTimerSheet
import com.rostomusic.app.ui.common.SpeedSheet
import com.rostomusic.app.ui.common.TextInputDialog
import com.rostomusic.app.ui.lyrics.LyricsPane
import com.rostomusic.app.ui.lyrics.LyricsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerScreen(nav: NavController, sharedScope: SharedTransitionScope, visibilityScope: AnimatedVisibilityScope) {
    val app = LocalApp.current
    val settings = LocalSettings.current
    val vm: PlayerViewModel = hiltViewModel()
    val lyricsVm: LyricsViewModel = hiltViewModel()
    val state by app.playerState.collectAsStateWithLifecycle()
    val panel by app.playerPanel.collectAsState()
    val coverAccent by vm.accent.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val wave by vm.wave.collectAsStateWithLifecycle()
    val lyrics by lyricsVm.loaded.collectAsStateWithLifecycle()
    val fetching by lyricsVm.fetching.collectAsStateWithLifecycle()
    val sleep by app.sleepTimer.state.collectAsState()
    val song = state.song ?: return
    val position by rememberPosition(app.player, song.id)
    val context = LocalContext.current
    val haptic = rememberHaptic()

    var showSleep by rememberSaveable { mutableStateOf(false) }
    var showSpeed by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var showOutput by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { lyricsVm.events.collect { app.message(it) } }
    // Reload when coming back from the editor.
    LaunchedEffect(Unit) { lyricsVm.refresh() }
    BackHandler(panel != PlayerPanel.NONE) { app.playerPanel.value = PlayerPanel.NONE }

    val hasLyrics = lyrics?.lyrics?.isEmpty == false

    val backdrop = settings.playerVisual != PlayerVisual.VINYL
    var infoTop by remember { mutableFloatStateOf(0f) }
    AccentScope(coverAccent) {
      // The cover fills the top of the screen and fades into its own colour, which covers the page.
      val tone = rememberCoverTone(if (backdrop) song.artUri else null, Rosto.colors.background)
      val pageBg = if (backdrop) tone else Rosto.colors.background
      // The status bar icons follow the colour under them (see App).
      SideEffect { app.playerTone.value = pageBg }
      DisposableEffect(Unit) { onDispose { app.playerTone.value = null } }
      ToneScope(pageBg) {
        val c = Rosto.colors
        Box(Modifier.fillMaxSize().background(c.background)) {
            if (backdrop) {
                // The picture stops where the title starts, so the text never sits on the image edge.
                val density = androidx.compose.ui.platform.LocalDensity.current
                val height = if (infoTop > 0f) with(density) { infoTop.toDp() } else 420.dp
                Box(Modifier.fillMaxWidth().height(height + Space.x6)) {
                    // Track change: crossfade with a 2 px blur that bridges the two pictures (API 31+).
                    val blurOk = !LocalReduceMotion.current && android.os.Build.VERSION.SDK_INT >= 31
                    var blurring by remember { mutableStateOf(false) }
                    LaunchedEffect(song.id) { blurring = true; kotlinx.coroutines.delay(320); blurring = false }
                    androidx.compose.animation.Crossfade(song, animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "backdrop") { s ->
                        CoverArt(s, Modifier.fillMaxSize().blur(if (blurOk && blurring) 2.dp else 0.dp), radius = 0.dp)
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to c.background.copy(alpha = 0.85f), 0.18f to Color.Transparent,
                                0.6f to Color.Transparent, 0.85f to c.background.copy(alpha = 0.85f), 1f to c.background,
                            )
                        )
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(hasLyrics) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total > 140f) app.collapsePlayer()
                                else if (total < -140f) app.playerPanel.value = if (hasLyrics) PlayerPanel.LYRICS else PlayerPanel.QUEUE
                            },
                        ) { _, dy -> total += dy }
                    }
            ) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.x1), verticalAlignment = Alignment.CenterVertically) {
                    IconBtn(R.drawable.ic_chevron_down, stringResource(R.string.cd_collapse_player), { app.collapsePlayer() })
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            song.album.ifEmpty { stringResource(R.string.unknown_album) }, style = Rosto.type.label,
                            color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconBtn(R.drawable.ic_ellipsis_vertical, stringResource(R.string.more_options), { showMore = true })
                }

                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val isWidePlayer = maxWidth >= 600.dp
                    val controlsSection: @Composable (Modifier) -> Unit = { colMod ->
                        Column(colMod.onGloballyPositioned { infoTop = it.positionInRoot().y }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    song.title, style = MaterialTheme.typography.headlineMediumEmphasized, color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false).basicMarquee(iterations = if (LocalReduceMotion.current) 0 else 1),
                                )
                                if (com.rostomusic.app.core.design.LocalSongQuality.current[song.id]?.explicit == true) {
                                    Spacer(Modifier.width(Space.x2))
                                    com.rostomusic.app.core.design.ExplicitBadge()
                                }
                            }
                            Text(
                                song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.body,
                                color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconBtn(
                            if (song.favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                            stringResource(if (song.favorite) R.string.remove_favorite else R.string.add_favorite),
                            { haptic(); app.toggleFavorite(song) },
                            tint = c.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(Space.x2))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2), verticalAlignment = Alignment.CenterVertically) {
                        // Two chips at most; bitrate, Hi-Res, EP and genre are on the Infos page.
                        Badge(song.format)
                        ReleaseInfo.dolbyLabel(details?.codecMime)?.let { DolbyBadge(it, small = true) }
                    }
                    Spacer(Modifier.height(Space.x3))

                    // Photo 1 : barre de progression ondulée encadrée par les deux compteurs sur la même ligne
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        ) {
                            Text(
                                text = formatDuration(position),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (settings.progressStyle == ProgressStyle.WAVE) {
                                    WaveSeekBar(wave, position, state.duration, { app.player.seekTo(it) })
                                } else {
                                    LineSeekBar(position, state.duration, { app.player.seekTo(it) }, playing = state.isPlaying)
                                }
                            }
                            val timeLabel = stringResource(R.string.cd_toggle_time)
                            Text(
                                text = if (settings.showRemaining) "-" + formatDuration((state.duration - position).coerceAtLeast(0)) else formatDuration(state.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable(role = Role.Button) { app.update { setShowRemaining(!settings.showRemaining) } }
                                    .semantics { contentDescription = timeLabel },
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.x4))

                    // Photo 1 : 3 gros boutons squircle (précédent tonal, play/pause filled haute emphase, suivant tonal)
                    Transport(state, onHaptic = haptic)
                    Spacer(Modifier.height(Space.x3))

                    // Photo 1 : rangée de 3 pilules secondaires (répéter circulaire + 2 pilules larges aléatoire / paroles)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val repeatIcon = if (state.repeat == QueueOps.Repeat.ONE) R.drawable.ic_repeat_1 else R.drawable.ic_repeat
                        val repeatDesc = stringResource(
                            when (state.repeat) {
                                QueueOps.Repeat.OFF -> R.string.cd_repeat_off
                                QueueOps.Repeat.ALL -> R.string.cd_repeat_all
                                QueueOps.Repeat.ONE -> R.string.cd_repeat_one
                            }
                        )
                        val repeatActive = state.repeat != QueueOps.Repeat.OFF
                        Surface(
                            onClick = { haptic(); app.player.cycleRepeat() },
                            shape = CircleShape,
                            color = if (repeatActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { role = Role.Button; contentDescription = repeatDesc },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                RIcon(repeatIcon, null, tint = if (repeatActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            }
                        }

                        val shuffleDesc = stringResource(if (state.shuffle) R.string.cd_shuffle_on else R.string.cd_shuffle_off)
                        Surface(
                            onClick = { haptic(); app.player.toggleShuffle() },
                            shape = CircleShape,
                            color = if (state.shuffle) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .semantics { role = Role.Button; contentDescription = shuffleDesc },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                RIcon(R.drawable.ic_shuffle, null, tint = if (state.shuffle) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            }
                        }

                        val lyricsDesc = stringResource(R.string.lyrics)
                        Surface(
                            onClick = { app.playerPanel.value = PlayerPanel.LYRICS },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .semantics { role = Role.Button; contentDescription = lyricsDesc },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                RIcon(R.drawable.ic_captions, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.x4))

                    // Photo 1 : 2 chips compactes aux extrémités gauche et droite (Sortie audio & File d'attente)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val outputLabel = stringResource(R.string.audio_output)
                        Surface(
                            onClick = { showOutput = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.semantics { role = Role.Button; contentDescription = outputLabel },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Space.x3, vertical = Space.x2),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.x2),
                            ) {
                                RIcon(R.drawable.ic_speaker, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                                Text(outputLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        val queueLabel = stringResource(R.string.queue)
                        Surface(
                            onClick = { app.playerPanel.value = PlayerPanel.QUEUE },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.semantics { role = Role.Button; contentDescription = queueLabel },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Space.x3, vertical = Space.x2),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.x2),
                            ) {
                                RIcon(R.drawable.ic_list_music, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                                Text(queueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                        }
                    }
                    if (isWidePlayer) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = Space.x6),
                            horizontalArrangement = Arrangement.spacedBy(Space.x6),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerVisualArea(
                                song = song,
                                queue = state.queue,
                                queueIndex = state.index,
                                playing = state.isPlaying,
                                visual = settings.playerVisual,
                                sharedScope = sharedScope,
                                visibilityScope = visibilityScope,
                                backdrop = backdrop,
                                onNext = { app.player.next() },
                                onPrevious = { app.player.previous() },
                                onDoubleTap = { haptic(); app.toggleFavorite(song) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            controlsSection(Modifier.weight(1f))
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            PlayerVisualArea(
                                song = song,
                                queue = state.queue,
                                queueIndex = state.index,
                                playing = state.isPlaying,
                                visual = settings.playerVisual,
                                sharedScope = sharedScope,
                                visibilityScope = visibilityScope,
                                backdrop = backdrop,
                                onNext = { app.player.next() },
                                onPrevious = { app.player.previous() },
                                onDoubleTap = { haptic(); app.toggleFavorite(song) },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            controlsSection(Modifier.padding(horizontal = Space.x6))
                        }
                    }
                }
                Spacer(Modifier.height(Space.x6))
            }

            val reduce = LocalReduceMotion.current
            AnimatedVisibility(
                visible = panel != PlayerPanel.NONE,
                enter = slideInVertically(if (reduce) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()) { it } +
                    fadeIn(if (reduce) snap() else MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = slideOutVertically(if (reduce) snap() else MaterialTheme.motionScheme.fastSpatialSpec()) { it } +
                    fadeOut(if (reduce) snap() else MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
                    if (panel == PlayerPanel.LYRICS) {
                        // Photo 2 : poignée de glissement + pochette miniature + titre/artiste + bouton options
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Space.x2, bottom = Space.x1)
                                .clickable(role = Role.Button) { app.playerPanel.value = PlayerPanel.NONE },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.screen, vertical = Space.x2),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.x3),
                        ) {
                            CoverArt(song, Modifier.size(44.dp), Radius.small)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (com.rostomusic.app.core.design.LocalSongQuality.current[song.id]?.explicit == true) {
                                        Spacer(Modifier.width(6.dp))
                                        com.rostomusic.app.core.design.ExplicitBadge()
                                    }
                                }
                                Text(
                                    text = song.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconBtn(
                                icon = R.drawable.ic_ellipsis,
                                contentDescription = stringResource(R.string.more_options),
                                onClick = { showMore = true },
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 40.dp,
                            )
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = Space.x1, end = Space.x4), verticalAlignment = Alignment.CenterVertically) {
                            IconBtn(R.drawable.ic_chevron_down, stringResource(R.string.close), { app.playerPanel.value = PlayerPanel.NONE })
                            Spacer(Modifier.weight(1f))
                            Pill(stringResource(R.string.lyrics), panel == PlayerPanel.LYRICS, { app.playerPanel.value = PlayerPanel.LYRICS })
                            Spacer(Modifier.width(Space.x2))
                            Pill(stringResource(R.string.queue), panel == PlayerPanel.QUEUE, { app.playerPanel.value = PlayerPanel.QUEUE })
                        }
                    }
                    when (panel) {
                        PlayerPanel.QUEUE -> QueuePanel(state)
                        else -> LyricsPane(
                            loaded = lyrics,
                            positionMs = position,
                            fetching = fetching,
                            onSeek = { app.player.seekTo(it) },
                            onEdit = {
                                app.collapsePlayer()
                                nav.navigate(Routes.lyricsEditor(song.id))
                            },
                            onFetch = { lyricsVm.fetchOnline() },
                            onOffset = { lyricsVm.nudgeOffset(it) },
                        )
                    }
                }
            }
        }
      }
    }

    if (showOutput) OutputSheet(song) { showOutput = false }
    if (showSleep) SleepTimerSheet { showSleep = false }
    if (showSpeed) SpeedSheet { showSpeed = false }
    if (showBookmarks) BookmarksSheet(vm, position) { showBookmarks = false }
    if (showMore) {
        RostoSheet({ showMore = false }) {
            SheetTitle(song.title, song.artist.ifEmpty { stringResource(R.string.unknown_artist) })
            Column(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
                SheetItem(
                    R.drawable.ic_timer, stringResource(R.string.sleep_timer), { showMore = false; showSleep = true },
                    trailing = sleepStatus(sleep),
                )
                SheetItem(
                    R.drawable.ic_sliders_horizontal, stringResource(R.string.equalizer),
                    { showMore = false; app.collapsePlayer(); nav.navigate(Routes.EQUALIZER) },
                    trailing = stringResource(if (settings.eqEnabled) R.string.on else R.string.off),
                )
                SheetItem(
                    R.drawable.ic_gauge, stringResource(R.string.playback_speed), { showMore = false; showSpeed = true },
                    trailing = formatSpeed(state.speed),
                )
                SheetItem(R.drawable.ic_share_2, stringResource(R.string.share), { showMore = false; app.share(context, song) })
                SheetItem(R.drawable.ic_bookmark, stringResource(R.string.bookmarks), { showMore = false; showBookmarks = true })
                SheetItem(R.drawable.ic_rotate_ccw, stringResource(R.string.rewind_10), { app.player.seekBy(-10_000) })
                SheetItem(R.drawable.ic_rotate_cw, stringResource(R.string.forward_10), { app.player.seekBy(10_000) })
                SheetItem(
                    if (settings.playerVisual == PlayerVisual.VINYL) R.drawable.ic_image else R.drawable.ic_disc_3,
                    stringResource(if (settings.playerVisual == PlayerVisual.VINYL) R.string.show_cover else R.string.show_vinyl),
                    {
                        showMore = false
                        app.update { setPlayerVisual(if (settings.playerVisual == PlayerVisual.VINYL) PlayerVisual.COVER else PlayerVisual.VINYL) }
                    },
                )
                SheetItem(R.drawable.ic_ellipsis, stringResource(R.string.more_song_actions), {
                    showMore = false
                    app.songMenu.value = SongMenu(song)
                })
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerVisualArea(
    song: Song,
    queue: List<Song>,
    queueIndex: Int,
    playing: Boolean,
    visual: PlayerVisual,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    backdrop: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = queue.ifEmpty { listOf(song) }
    val safeIndex = queueIndex.coerceIn(0, pages.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex) { pages.size }

    LaunchedEffect(safeIndex, pages.size) {
        if (pagerState.currentPage != safeIndex && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                page = safeIndex,
                animationSpec = if (LocalReduceMotion.current) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
            )
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settled = pagerState.settledPage
        if (settled == safeIndex + 1) onNext()
        else if (settled == safeIndex - 1) onPrevious()
    }

    Box(
        modifier = modifier
            .padding(vertical = Space.x4)
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) },
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 56.dp),
            pageSpacing = Space.x3,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val pageSong = pages[page]
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .absoluteValue
                .coerceIn(0f, 1f)
            val scale = floatLerp(1f, 0.86f, offset)
            val corner = lerp(12.dp, 28.dp, offset)
            val elevation = lerp(8.dp, 2.dp, offset)
            val shared = if (page == safeIndex) {
                with(sharedScope) {
                    Modifier.sharedElement(rememberSharedContentState("cover"), visibilityScope)
                }
            } else {
                Modifier
            }
            Card(
                shape = RoundedCornerShape(corner),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset * 12.dp.toPx()
                    }
                    .then(shared),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (visual == PlayerVisual.VINYL && page == safeIndex) {
                        Vinyl(pageSong, pageSong.album.ifEmpty { pageSong.title }, playing, Modifier.fillMaxSize())
                        Tonearm(playing, Modifier.fillMaxSize())
                    } else {
                        CoverArt(pageSong, Modifier.fillMaxSize(), radius = corner)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Transport(state: PlayerState, onHaptic: () -> Unit) {
    val app = LocalApp.current
    val prevDesc = stringResource(R.string.cd_previous)
    val playDesc = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play)
    val nextDesc = stringResource(R.string.cd_next)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            Surface(
                onClick = { onHaptic(); app.player.previous() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .semantics { role = Role.Button; contentDescription = prevDesc },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RIcon(R.drawable.ic_skip_back_filled, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 26.dp)
                }
            }

            // 1 seul bouton haute emphase (Filled) par écran
            Surface(
                onClick = { onHaptic(); app.player.toggle() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1.35f)
                    .height(76.dp)
                    .semantics { role = Role.Button; contentDescription = playDesc },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RIcon(
                        if (state.isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 30.dp,
                    )
                }
            }

            Surface(
                onClick = { onHaptic(); app.player.next() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .semantics { role = Role.Button; contentDescription = nextDesc },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RIcon(R.drawable.ic_skip_forward_filled, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 26.dp)
                }
            }
        }
    }
}

/** Remaining time of the sleep timer, shown next to its menu entry. */
@Composable
private fun sleepStatus(state: SleepState): String {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state is SleepState.Until) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    return when (state) {
        SleepState.Off -> stringResource(R.string.off)
        SleepState.EndOfTrack -> stringResource(R.string.sleep_end_of_track_short)
        is SleepState.Until -> formatDuration((state.endAt - now).coerceAtLeast(0))
    }
}

private fun formatSpeed(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else "%.2f".format(speed).trimEnd('0')) + "×"

@Composable
private fun BookmarksSheet(vm: PlayerViewModel, position: Long, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    RostoSheet(onDismiss) {
        SheetTitle(stringResource(R.string.bookmarks), stringResource(R.string.bookmarks_hint))
        Column(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
            SheetItem(
                R.drawable.ic_plus, stringResource(R.string.bookmark_add_at, formatDuration(position)), { adding = true },
                tint = Rosto.colors.accentText,
            )
            for (b in bookmarks) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        SheetItem(R.drawable.ic_bookmark, b.label.ifEmpty { formatDuration(b.positionMs) }, {
                            app.player.seekTo(b.positionMs)
                            onDismiss()
                        }, trailing = formatDuration(b.positionMs))
                    }
                    IconBtn(R.drawable.ic_trash_2, stringResource(R.string.delete), { vm.deleteBookmark(b) }, tint = Rosto.colors.textMuted, iconSize = 18.dp)
                    Spacer(Modifier.width(Space.x2))
                }
            }
        }
    }
    if (adding) {
        TextInputDialog(
            title = stringResource(R.string.bookmark_new),
            placeholder = stringResource(R.string.bookmark_label),
            confirmLabel = stringResource(R.string.save),
            initial = formatDuration(position),
            onDismiss = { adding = false },
            onConfirm = {
                vm.addBookmark(it)
                adding = false
            },
        )
    }
}
