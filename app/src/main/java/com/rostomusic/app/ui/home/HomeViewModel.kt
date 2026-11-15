package com.rostomusic.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.ArtistInfoRepository
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.domain.HomeConfig
import com.rostomusic.app.domain.HomeLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class WeekStats(
    /** Minutes per day, oldest first, today last. */
    val days: List<Int> = List(7) { 0 },
    val dayStarts: List<Long> = emptyList(),
    val weekMs: Long = 0,
    val previousWeekMs: Long = 0,
    val todayMs: Long = 0,
)

data class TopArtist(val name: String, val albumId: Long, val songCount: Int, val listenedMs: Long)

data class RecentPlay(val song: Song, val playedAt: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    db: RostoDatabase,
    library: LibraryRepository,
    settings: SettingsStore,
    private val artistInfo: ArtistInfoRepository,
) : ViewModel() {
    private val dayMs = 86_400_000L
    private val startOfToday: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val layout: StateFlow<HomeLayout> = settings.flow.map { HomeConfig.decode(it.homeLayout) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeConfig.preset(HomeConfig.DEFAULT_PRESET))

    private val history = db.history().observeSince(startOfToday - 30 * dayMs)

    val week: StateFlow<WeekStats> = history.map { list ->
        val firstDay = startOfToday - 6 * dayMs
        val starts = (0..6).map { firstDay + it * dayMs }
        val days = starts.map { start -> (list.filter { it.playedAt in start until start + dayMs }.sumOf { it.listenedMs } / 60_000).toInt() }
        WeekStats(
            days = days,
            dayStarts = starts,
            weekMs = list.filter { it.playedAt >= firstDay }.sumOf { it.listenedMs },
            previousWeekMs = list.filter { it.playedAt in firstDay - 7 * dayMs until firstDay }.sumOf { it.listenedMs },
            todayMs = list.filter { it.playedAt >= startOfToday }.sumOf { it.listenedMs },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekStats())

    /** Most listened artist over the last 30 days, or over all time when the month is empty. */
    val topArtist: StateFlow<TopArtist?> = combine(history, library.songsById) { list, byId ->
        val month = list.groupBy { byId[it.songId]?.artist.orEmpty() }
            .filterKeys { it.isNotEmpty() }
            .mapValues { (_, e) -> e.sumOf { it.listenedMs } }
        val name = month.maxByOrNull { it.value }?.key
            ?: byId.values.filter { it.artist.isNotEmpty() }.groupBy { it.artist }.maxByOrNull { (_, s) -> s.sumOf { it.listenedMs } }?.key
            ?: return@combine null
        val songs = byId.values.filter { it.artist == name }
        TopArtist(name, songs.firstOrNull()?.albumId ?: -1, songs.size, month[name] ?: songs.sumOf { it.listenedMs })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Photo of the top artist, when online artist info is on. */
    val topArtistImage: StateFlow<String?> = combine(topArtist.map { it?.name }.distinctUntilChanged(), settings.flow.map { it.artistInfoOnline }.distinctUntilChanged()) { n, on -> n to on }
        .mapLatest { (name, on) -> if (on && name != null) artistInfo.get(name)?.imageUrl?.takeIf { it.isNotEmpty() } else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentPlays: StateFlow<List<RecentPlay>> = combine(db.history().observeRecent(6), library.songsById) { list, byId ->
        list.mapNotNull { h -> byId[h.songId]?.let { RecentPlay(it, h.playedAt) } }.distinctBy { it.song.id }.take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
