package com.rostomusic.app.ui.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.LocalReduceMotion
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.repo.LoadedLyrics
import com.rostomusic.app.data.repo.LyricsSource
import com.rostomusic.app.data.settings.LyricsAlign
import com.rostomusic.app.domain.ExplicitFilter
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.ui.LocalSettings

@Composable
fun lyricsSourceLabel(source: LyricsSource): String = stringResource(
    when (source) {
        LyricsSource.USER -> R.string.lyrics_source_user
        LyricsSource.ONLINE -> R.string.lyrics_source_online
        LyricsSource.TAG_SYNCED -> R.string.lyrics_source_tag_synced
        LyricsSource.LRC_FILE -> R.string.lyrics_source_file
        LyricsSource.TAG_PLAIN -> R.string.lyrics_source_tag
        LyricsSource.NONE -> R.string.lyrics_source_none
    }
)

/** Full lyrics: follows the song, lets the user scroll freely and tap a line to jump there. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsPane(
    loaded: LoadedLyrics?,
    positionMs: Long,
    fetching: Boolean,
    onSeek: (Long) -> Unit,
    onEdit: () -> Unit,
    onFetch: () -> Unit,
    onOffset: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rosto.colors
    val settings = LocalSettings.current
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        if (settings.keepScreenOn) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    if (loaded == null) {
        Column(modifier.fillMaxSize().padding(Space.x6), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
            repeat(6) { i -> Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.8f else 0.55f).height(18.dp).clip(CircleShape).background(c.card)) }
        }
        return
    }
    if (loaded.lyrics.isEmpty) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyState(
                R.drawable.ic_captions,
                stringResource(R.string.lyrics_empty_title),
                stringResource(if (fetching) R.string.lyrics_searching else R.string.lyrics_empty_body),
                actionLabel = stringResource(R.string.lyrics_add),
                onAction = onEdit,
            )
            if (settings.onlineLyrics && !fetching) {
                SecondaryButton(stringResource(R.string.lyrics_search_online), onFetch, icon = R.drawable.ic_globe)
            }
        }
        return
    }

    val shown = remember(loaded, settings.explicitLyrics) {
        if (settings.explicitLyrics) loaded.lyrics else ExplicitFilter.mask(loaded.lyrics)
    }
    val lines = shown.lines
    val synced = shown.synced
    val effective = positionMs + loaded.userOffsetMs
    val index = if (synced) LrcParser.indexAt(lines, effective) else -1
    val listState = rememberLazyListState()
    var following by remember(loaded.songId) { mutableStateOf(true) }
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) following = false }
    }
    LaunchedEffect(index, following, synced) {
        if (synced && following && index >= 0) {
            val offset = -(listState.layoutInfo.viewportSize.height / 3)
            if (reduceMotion) listState.scrollToItem(index, offset) else listState.animateScrollToItem(index, offset)
        }
    }

    val align = if (settings.lyricsAlign == LyricsAlign.CENTER) TextAlign.Center else TextAlign.Start
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = Space.screen, end = Space.x2), verticalAlignment = Alignment.CenterVertically) {
                Text(lyricsSourceLabel(loaded.source), style = Rosto.type.caption, color = c.textMuted, modifier = Modifier.weight(1f), maxLines = 1)
                if (synced) {
                    IconBtn(R.drawable.ic_minus, stringResource(R.string.lyrics_earlier), { onOffset(-100) }, tint = c.textSecondary, iconSize = 18.dp)
                    Text(
                        "%+.1f s".format(loaded.userOffsetMs / 1000f), style = Rosto.type.label, color = c.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.width(52.dp),
                    )
                    IconBtn(R.drawable.ic_plus, stringResource(R.string.lyrics_later), { onOffset(100) }, tint = c.textSecondary, iconSize = 18.dp)
                }
                IconBtn(R.drawable.ic_square_pen, stringResource(R.string.edit_lyrics), onEdit, tint = c.textSecondary, iconSize = 20.dp)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Space.screen, end = Space.screen, top = Space.x6, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(Space.x4),
            ) {
                itemsIndexed(lines) { i, line ->
                    val current = i == index
                    val scale by animateFloatAsState(if (current) 1f else 0.94f, if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio = MaterialTheme.motionScheme.defaultSpatialDamping), label = "line")
                    val base = MaterialTheme.typography.headlineSmallEmphasized
                    val lineModifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(if (align == TextAlign.Center) 0.5f else 0f, 0.5f)
                        }
                        .then(if (synced) Modifier.clickable { following = true; onSeek((line.timeMs - loaded.userOffsetMs).coerceAtLeast(0)) } else Modifier)
                    if (!synced) {
                        Text(line.text.ifEmpty { " " }, style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurface, textAlign = align, modifier = Modifier.fillMaxWidth())
                    } else if (current && line.words.isNotEmpty()) {
                        val text = buildAnnotatedString {
                            for (w in line.words) {
                                withStyle(SpanStyle(color = if (w.timeMs <= effective) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))) { append(w.text) }
                            }
                        }
                        Text(text, style = base, textAlign = align, modifier = lineModifier)
                    } else {
                        Text(
                            line.text.ifEmpty { "· · ·" }, style = base,
                            color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                            textAlign = align, modifier = lineModifier,
                        )
                    }
                }
            }
        }
        if (synced && !following) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Space.x8)
                    .height(Space.touch)
                    .clip(CircleShape)
                    .background(c.textPrimary)
                    .clickable(role = Role.Button) { following = true }
                    .padding(horizontal = Space.x5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.x2),
            ) {
                RIcon(R.drawable.ic_locate_fixed, null, tint = c.background, size = 18.dp)
                Text(stringResource(R.string.lyrics_back_to_current), style = Rosto.type.label, color = c.background)
            }
        }
    }
}

@Composable
fun Spacer4() = Spacer(Modifier.height(Space.x4))
