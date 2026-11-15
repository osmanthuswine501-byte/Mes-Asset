package com.rostomusic.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.LocalReduceMotion
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.pressScale
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.rememberHaptic
import com.rostomusic.app.playback.PlayerConnection
import com.rostomusic.app.playback.PlayerState
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.Routes
import kotlinx.coroutines.delay

val BottomBarHeight = 80.dp
val MiniPlayerHeight = 72.dp

/** Polls the playback position a few times per second while the caller is on screen. */
@Composable
fun rememberPosition(player: PlayerConnection, songId: Long?): State<Long> =
    produceState(player.position, songId) {
        while (true) {
            value = player.position
            delay(200)
        }
    }

/** A record that spins while music plays and coasts to a stop on pause. */
@Composable
fun Vinyl(song: Song?, seed: String, spinning: Boolean, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    val reduce = LocalReduceMotion.current
    val angle = remember { Animatable(0f) }
    var wasSpinning by remember { mutableStateOf(false) }
    LaunchedEffect(spinning, reduce) {
        if (reduce) return@LaunchedEffect
        if (spinning) {
            wasSpinning = true
            while (true) {
                angle.snapTo(angle.value % 360f)
                angle.animateTo(angle.value + 360f, tween(9000, easing = LinearEasing))
            }
        } else if (wasSpinning) {
            wasSpinning = false
            angle.animateTo(angle.value + 28f, tween(900, easing = LinearOutSlowInEasing))
        }
    }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(c.vinyl, r)
                var f = 0.50f
                while (f < 0.97f) {
                    drawCircle(c.vinylGroove, r * f, style = Stroke(1f))
                    f += 0.045f
                }
            }
            if (song != null) CoverArt(song, Modifier.fillMaxSize(0.40f), radius = Radius.disc) else CoverArt(-1, seed, Modifier.fillMaxSize(0.40f), radius = Radius.disc)
            Box(Modifier.fillMaxSize(0.045f).clip(CircleShape).background(c.card))
        }
        // The light catching the record stays still while the record turns.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val inset = r * 0.04f
            val arcSize = Size((r - inset) * 2, (r - inset) * 2)
            val topLeft = Offset(center.x - r + inset, center.y - r + inset)
            drawArc(Color.White.copy(alpha = 0.06f), -62f, 34f, true, topLeft, arcSize)
            drawArc(Color.White.copy(alpha = 0.06f), 118f, 34f, true, topLeft, arcSize)
        }
    }
}

/** Tonearm drawn over the top right of the record; it lifts away on pause. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
fun Tonearm(onRecord: Boolean, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    val reduce = LocalReduceMotion.current
    val angle by animateFloatAsState(if (onRecord) 20f else -4f, if (reduce) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(), label = "arm")
    Canvas(modifier) {
        val pivot = Offset(size.width * 0.90f, size.height * 0.07f)
        val length = size.height * 0.50f
        val stroke = size.width * 0.018f
        rotate(angle, pivot) {
            drawLine(c.textMuted, pivot, Offset(pivot.x, pivot.y + length), stroke, StrokeCap.Round)
            drawRoundRect(
                c.textSecondary,
                topLeft = Offset(pivot.x - stroke * 1.6f, pivot.y + length - stroke),
                size = Size(stroke * 3.2f, stroke * 5.5f),
                cornerRadius = CornerRadius(stroke, stroke),
            )
        }
        drawCircle(c.card, size.width * 0.05f, pivot)
        drawCircle(c.textMuted, size.width * 0.05f, pivot, style = Stroke(stroke * 0.6f))
        drawCircle(c.textMuted, size.width * 0.016f, pivot)
    }
}

/** Shared gesture surface for the two progress styles: tap or drag to seek. */
@Composable
private fun SeekSurface(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    draw: DrawScope.(fraction: Float) -> Unit,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val duration = durationMs.coerceAtLeast(1)
    val fraction = if (dragFraction >= 0f) dragFraction else (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentDuration by rememberUpdatedState(duration)
    val description = stringResource(R.string.cd_seek)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = description
                stateDescription = "${com.rostomusic.app.core.design.formatDuration((fraction * currentDuration).toLong())} / ${com.rostomusic.app.core.design.formatDuration(currentDuration)}"
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { target ->
                    currentOnSeek((target.coerceIn(0f, 1f) * currentDuration).toLong())
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentOnSeek(((offset.x / size.width).coerceIn(0f, 1f) * currentDuration).toLong())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        if (dragFraction >= 0f) currentOnSeek((dragFraction * currentDuration).toLong())
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                ) { change, _ ->
                    change.consume()
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }
    ) { draw(fraction) }
}

@Composable
fun LineSeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, playing: Boolean = false) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHigh
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    // The wave flows while playing and settles flat on pause.
    val phase by androidx.compose.animation.core.rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 6.2831855f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.LinearEasing)), label = "phase",
    )
    val amplitude by androidx.compose.animation.core.animateFloatAsState(if (playing) 1f else 0f, MaterialTheme.motionScheme.fastEffectsSpec(), label = "amp")
    SeekSurface(positionMs, durationMs, onSeek, 36.dp, modifier) { fraction ->
        val y = size.height / 2
        val track = 4.dp.toPx()
        val pad = 6.dp.toPx()
        val x = pad + (size.width - pad * 2) * fraction
        val gap = 6.dp.toPx()
        // Remaining track: straight, starts after the handle.
        if (x + gap < size.width - pad) drawLine(inactiveColor, Offset(x + gap, y), Offset(size.width - pad, y), track, StrokeCap.Round)
        // Played part: a gentle wave.
        val amp = 3.dp.toPx() * amplitude
        val wave = 20.dp.toPx()
        val path = androidx.compose.ui.graphics.Path()
        var px = pad
        val end = (x - gap).coerceAtLeast(pad)
        path.moveTo(px, y)
        while (px < end) {
            px = (px + 2.dp.toPx()).coerceAtMost(end)
            // The wave grows from the line at both ends, so a short played part never shows a hook.
            val ramp = minOf((px - pad) / wave, (end - px) / (wave / 2) + 0.0f).coerceIn(0f, 1f)
            path.lineTo(px, y + amp * ramp * kotlin.math.sin((px - pad) / wave * 6.2831855f - phase))
        }
        drawPath(path, activeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = track, cap = StrokeCap.Round))
        // Handle: a tall pill, as in the Expressive slider.
        drawRoundRect(activeColor, Offset(x - 2.dp.toPx(), y - 11.dp.toPx()), Size(4.dp.toPx(), 22.dp.toPx()), CornerRadius(2.dp.toPx()))
    }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
fun WaveSeekBar(bars: FloatArray, positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHigh
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    SeekSurface(positionMs, durationMs, onSeek, 56.dp, modifier) { fraction ->
        if (bars.isEmpty()) return@SeekSurface
        val slot = size.width / bars.size
        val w = (slot * 0.55f).coerceAtLeast(2f)
        for ((i, v) in bars.withIndex()) {
            val h = (size.height * v.coerceIn(0.08f, 1f)).coerceAtLeast(w)
            val x = slot * i + (slot - w) / 2
            val played = (i + 0.5f) / bars.size <= fraction
            drawRoundRect(
                if (played) activeColor else inactiveColor,
                topLeft = Offset(x, (size.height - h) / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2, w / 2),
            )
        }
    }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
fun MiniPlayer(
    state: PlayerState,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    onExpand: () -> Unit,
) {
    val song = state.song ?: return
    val app = LocalApp.current
    val c = Rosto.colors
    val position by rememberPosition(app.player, song.id)
    val explicit = com.rostomusic.app.core.design.LocalSongQuality.current[song.id]?.explicit == true
    Column(
        Modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .clickable(role = Role.Button, onClick = onExpand)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total < -40f) onExpand() }) { _, dy -> total += dy }
            }
    ) {
        val fraction = if (state.duration > 0) (position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
        val animatedProgress by animateFloatAsState(
            targetValue = fraction,
            animationSpec = if (LocalReduceMotion.current) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "wavySync",
        )
        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
        androidx.compose.material3.LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.padding(start = Space.x6, end = Space.x6, top = Space.x2).fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            amplitude = { if (state.isPlaying) 1f else 0f },
            stopSize = 0.dp,
        )
        Row(Modifier.weight(1f).padding(start = Space.x4, end = Space.x2), verticalAlignment = Alignment.CenterVertically) {
            val coverModifier = with(sharedScope) {
                Modifier.size(52.dp).sharedElement(rememberSharedContentState("cover"), visibilityScope)
            }
            CoverArt(song, coverModifier)
            Spacer(Modifier.width(Space.x3))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        song.title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (explicit) {
                        Spacer(Modifier.width(6.dp))
                        com.rostomusic.app.core.design.ExplicitBadge()
                    }
                }
                Text(
                    song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.label,
                    color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Space.x2))
            PlayPauseMorph(state.isPlaying) { app.player.toggle() }
            IconBtn(R.drawable.ic_skip_forward_filled, stringResource(R.string.cd_next), { app.player.next() }, tint = c.textPrimary, iconSize = 26.dp)
        }
    }
}

/** Material 3 Expressive play button: a circle when paused, morphing to a rounded square while playing. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayPauseMorph(playing: Boolean, size: Dp = 52.dp, onClick: () -> Unit) {
    val c = Rosto.colors
    val reduce = LocalReduceMotion.current
    val press = remember { MutableInteractionSource() }
    val isPressed by press.collectIsPressedAsState()
    val pressedCorner = if (size >= 72.dp) 16.dp else 12.dp
    val activeCorner = if (size >= 72.dp) 28.dp else size * 0.29f
    val targetCorner = when {
        isPressed -> pressedCorner
        playing -> activeCorner
        else -> size / 2
    }
    val corner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = if (reduce) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "morph",
    )
    val iconSize = if (size >= 72.dp) 32.dp else size * 0.46f
    val label = stringResource(if (playing) R.string.nav_pause else R.string.nav_play)
    Box(
        Modifier.pressScale(press).size(size).clip(RoundedCornerShape(corner)).background(c.accent)
            .clickable(press, androidx.compose.foundation.LocalIndication.current, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        RIcon(if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_filled, null, tint = c.onAccent, size = iconSize)
    }
}

/** A bottom tab: [pattern] is its route template, [target] the route to open. */
data class Tab(val pattern: String, val target: String)

private data class NavTab(val tab: Tab, val label: Int, val icon: Int, val iconSelected: Int)

private val NAV_TABS = listOf(
    NavTab(Tab(Routes.HOME, Routes.HOME), R.string.nav_home, R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    NavTab(Tab(Routes.ALBUMS, Routes.ALBUMS), R.string.nav_albums, R.drawable.ic_nav_albums, R.drawable.ic_nav_albums_filled),
    NavTab(Tab(Routes.SONGS, Routes.SONGS), R.string.nav_songs, R.drawable.ic_nav_songs, R.drawable.ic_nav_songs_filled),
    NavTab(Tab(Routes.LIBRARY, Routes.library()), R.string.nav_library, R.drawable.ic_nav_library, R.drawable.ic_nav_library_filled),
    NavTab(Tab(Routes.ARTISTS, Routes.ARTISTS), R.string.nav_artists, R.drawable.ic_nav_artists, R.drawable.ic_nav_artists_filled),
)

@Composable
fun BottomBar(route: String?, onNavigate: (Tab) -> Unit) {
    val haptic = rememberHaptic()
    androidx.compose.material3.NavigationBar(
        modifier = Modifier.fillMaxWidth().height(BottomBarHeight),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        windowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
    ) {
        for (t in NAV_TABS) {
            val selected = route == t.tab.pattern
            val label = stringResource(t.label)
            androidx.compose.material3.NavigationBarItem(
                modifier = Modifier.padding(horizontal = 0.dp),
                selected = selected,
                onClick = {
                    if (!selected) haptic()
                    onNavigate(t.tab)
                },
                icon = {
                    RIcon(
                        if (selected) t.iconSelected else t.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 24.dp,
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * A tab of the bar: icon only (the name stays as its accessibility label). The selected one gets a
 * pill that stretches out from the centre behind the icon and a filled icon. The icon keeps its size
 * and place; no scale on press.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavItem(t: NavTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Rosto.colors
    val reduce = LocalReduceMotion.current
    val haptic = rememberHaptic()
    val p by animateFloatAsState(
        if (selected) 1f else 0f,
        if (reduce) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "tab",
    )
    val fade by animateFloatAsState(
        if (selected) 1f else 0f,
        if (reduce) snap() else MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tabFade",
    )
    val tint by androidx.compose.animation.animateColorAsState(
        if (selected) c.textPrimary else c.textSecondary,
        if (reduce) snap() else MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tabTint",
    )
    val label = stringResource(t.label)
    Column(
        modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
            ) { if (!selected) haptic(); onClick() }
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(Modifier.width(64.dp).height(32.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = 0.5f + 0.5f * p; alpha = fade }
                    .clip(CircleShape)
                    .background(c.accentContainer)
            )
            RIcon(t.icon, null, Modifier.graphicsLayer { alpha = 1f - fade }, tint = tint, size = 24.dp)
            RIcon(t.iconSelected, null, Modifier.graphicsLayer { alpha = fade }, tint = tint, size = 24.dp)
        }
    }
}

/** Adaptive M3 NavigationRail for Medium / Expanded window widths (>= 600dp). */
@Composable
fun SideNavRail(route: String?, onNavigate: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    androidx.compose.material3.NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Spacer(Modifier.weight(1f))
        for (t in NAV_TABS) {
            val selected = route == t.tab.pattern
            val label = stringResource(t.label)
            androidx.compose.material3.NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(t.tab) },
                icon = {
                    RIcon(
                        if (selected) t.iconSelected else t.icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else c.textSecondary,
                        size = 24.dp,
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = false,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
