package com.rostomusic.app.ui.stats

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.rostomusic.app.R
import com.rostomusic.app.core.design.BentoCard
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.TopBar
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.ui.LocalBottomInset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DayStat(val label: String, val minutes: Int, val today: Boolean)
data class Ranked(val title: String, val subtitle: String, val albumId: Long, val listenedMs: Long)
data class HistoryItem(val song: Song, val playedAt: Long)

data class StatsUi(
    val totalMs: Long = 0,
    val todayMs: Long = 0,
    val weekMs: Long = 0,
    val days: List<DayStat> = emptyList(),
    val topSongs: List<Ranked> = emptyList(),
    val topArtists: List<Ranked> = emptyList(),
    val topAlbums: List<Ranked> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(db: RostoDatabase, library: LibraryRepository) : ViewModel() {
    private val weekStart: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -6)
    }.timeInMillis

    val ui: StateFlow<StatsUi> = combine(
        db.history().observeTotalListened(),
        db.history().observeSince(weekStart),
        db.history().observeRecent(40),
        library.songs,
    ) { total, week, recent, songs ->
        val dayMs = 86_400_000L
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        val days = (0..6).map { i ->
            val start = weekStart + i * dayMs
            val ms = week.filter { it.playedAt >= start && it.playedAt < start + dayMs }.sumOf { it.listenedMs }
            DayStat(fmt.format(Date(start)).trimEnd('.'), (ms / 60_000).toInt(), i == 6)
        }
        val byId = songs.associateBy { it.id }
        val listened = songs.filter { it.listenedMs > 0 }
        StatsUi(
            totalMs = total,
            todayMs = week.filter { it.playedAt >= weekStart + 6 * dayMs }.sumOf { it.listenedMs },
            weekMs = week.sumOf { it.listenedMs },
            days = days,
            topSongs = listened.sortedByDescending { it.listenedMs }.take(5).map { Ranked(it.title, it.artist, it.albumId, it.listenedMs) },
            topArtists = listened.groupBy { it.artist }.map { (name, s) -> Ranked(name, "", s.first().albumId, s.sumOf { it.listenedMs }) }
                .sortedByDescending { it.listenedMs }.take(5),
            topAlbums = listened.groupBy { it.albumId }.map { (id, s) -> Ranked(s.first().album, s.first().artist, id, s.sumOf { it.listenedMs }) }
                .sortedByDescending { it.listenedMs }.take(5),
            history = recent.mapNotNull { h -> byId[h.songId]?.let { HistoryItem(it, h.playedAt) } },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUi())
}

@Composable
private fun listenTime(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    return if (minutes >= 60) stringResource(R.string.hours_minutes, minutes / 60, minutes % 60) else pluralStringResource(R.plurals.minutes_count, minutes, minutes)
}

@Composable
fun StatsScreen(nav: NavController) {
    val vm: StatsViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = Rosto.colors
    val unknownArtist = stringResource(R.string.unknown_artist)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.stats), onBack = { nav.popBackStack() })
        if (ui.totalMs == 0L) {
            EmptyState(R.drawable.ic_chart_column, stringResource(R.string.stats_empty_title), stringResource(R.string.stats_empty_body))
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.screen, end = Space.screen, bottom = LocalBottomInset.current + Space.x4),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                    StatTile(stringResource(R.string.stats_today), listenTime(ui.todayMs), Modifier.weight(1f))
                    StatTile(stringResource(R.string.stats_total), listenTime(ui.totalMs), Modifier.weight(1f))
                }
            }
            item {
                BentoCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Space.x4)) {
                        Text(stringResource(R.string.stats_week), style = Rosto.type.heading, color = c.textPrimary)
                        Text(listenTime(ui.weekMs), style = Rosto.type.label, color = c.textSecondary)
                        Spacer(Modifier.height(Space.x4))
                        val summary = ui.days.joinToString { "${it.label} ${it.minutes} min" }
                        val max = (ui.days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
                        Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = summary }) {
                            val slot = size.width / ui.days.size.coerceAtLeast(1)
                            val w = slot * 0.5f
                            ui.days.forEachIndexed { i, d ->
                                val h = (size.height * d.minutes / max).coerceAtLeast(6f)
                                drawRoundRect(
                                    if (d.today) c.textPrimary else c.surfaceAlt,
                                    topLeft = Offset(slot * i + (slot - w) / 2, size.height - h),
                                    size = Size(w, h),
                                    cornerRadius = CornerRadius(w / 3, w / 3),
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.x2))
                        Row {
                            ui.days.forEach { d ->
                                Text(
                                    d.label, style = Rosto.type.caption, color = if (d.today) c.textPrimary else c.textMuted,
                                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            item { RankCard(stringResource(R.string.stats_top_songs), ui.topSongs, unknownArtist) }
            item { RankCard(stringResource(R.string.stats_top_artists), ui.topArtists, unknownArtist) }
            item { RankCard(stringResource(R.string.stats_top_albums), ui.topAlbums, unknownArtist) }
            if (ui.history.isNotEmpty()) {
                item { Text(stringResource(R.string.stats_history), style = Rosto.type.title, color = c.textPrimary, modifier = Modifier.padding(top = Space.x2)) }
                items(ui.history) { h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoverArt(h.song, Modifier.size(com.rostomusic.app.core.design.CoverSize.s), Radius.small)
                        Spacer(Modifier.width(Space.x3))
                        Column(Modifier.weight(1f)) {
                            Text(h.song.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(h.song.artist.ifEmpty { unknownArtist }, style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            DateUtils.getRelativeTimeSpanString(h.playedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                            style = Rosto.type.caption, color = c.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    BentoCard(modifier) {
        Column(Modifier.padding(Space.x4)) {
            Text(label, style = Rosto.type.label, color = c.textSecondary)
            Spacer(Modifier.height(Space.x2))
            Text(value, style = Rosto.type.title, color = c.textPrimary, maxLines = 2)
        }
    }
}

@Composable
private fun RankCard(title: String, rows: List<Ranked>, unknown: String) {
    if (rows.isEmpty()) return
    val c = Rosto.colors
    BentoCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.x4), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
            Text(title, style = Rosto.type.heading, color = c.textPrimary)
            rows.forEachIndexed { i, r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((i + 1).toString(), style = Rosto.type.label, color = c.textMuted, modifier = Modifier.width(20.dp))
                    CoverArt(r.albumId, r.title, Modifier.size(com.rostomusic.app.core.design.CoverSize.s), Radius.small)
                    Spacer(Modifier.width(Space.x3))
                    Column(Modifier.weight(1f)) {
                        Text(r.title.ifEmpty { unknown }, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (r.subtitle.isNotEmpty()) Text(r.subtitle, style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(listenTime(r.listenedMs), style = Rosto.type.label, color = c.textMuted)
                }
            }
        }
    }
}
