package com.rostomusic.app.ui.lyrics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.repo.LoadedLyrics
import com.rostomusic.app.data.repo.LyricsRepository
import com.rostomusic.app.data.repo.LyricsSource
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.domain.LyricLine
import com.rostomusic.app.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Lyrics of the song being played. */
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repo: LyricsRepository,
    private val player: PlayerConnection,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _loaded = MutableStateFlow<LoadedLyrics?>(null)
    /** Null while loading. */
    val loaded: StateFlow<LoadedLyrics?> = _loaded

    private val _fetching = MutableStateFlow(false)
    val fetching: StateFlow<Boolean> = _fetching

    val events = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(player.state.map { it.song?.id }.distinctUntilChanged(), reload) { id, _ -> id }.collectLatest { id ->
                _loaded.value = null
                val song = id?.let { library.song(it) }
                _loaded.value = if (song == null) LoadedLyrics.empty(id ?: -1) else repo.load(song)
            }
        }
    }

    fun refresh() {
        reload.value++
    }

    fun nudgeOffset(deltaMs: Long) {
        val cur = _loaded.value ?: return
        val next = cur.userOffsetMs + deltaMs
        _loaded.value = cur.copy(userOffsetMs = next)
        viewModelScope.launch { repo.setOffset(cur.songId, next) }
    }

    fun fetchOnline() {
        val song = player.state.value.song ?: return
        if (_fetching.value) return
        viewModelScope.launch {
            _fetching.value = true
            val text = repo.fetchOnline(song)
            _fetching.value = false
            if (text == null) events.emit(R.string.lyrics_not_found)
            else {
                repo.save(song.id, text, LyricsSource.ONLINE)
                refresh()
            }
        }
    }
}

data class SyncLine(val text: String, val timeMs: Long?)

/** Lyrics editor for one song, opened from a menu. */
@HiltViewModel
class LyricsEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: LyricsRepository,
    private val library: LibraryRepository,
    val player: PlayerConnection,
) : ViewModel() {
    private val songId: Long = savedState.get<Long>("id") ?: -1

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song

    val text = MutableStateFlow("")
    private val _source = MutableStateFlow(LyricsSource.NONE)
    val source: StateFlow<LyricsSource> = _source

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** Non-null while the tap-to-sync mode is open. */
    private val _sync = MutableStateFlow<List<SyncLine>?>(null)
    val sync: StateFlow<List<SyncLine>?> = _sync
    private val _syncIndex = MutableStateFlow(0)
    val syncIndex: StateFlow<Int> = _syncIndex

    val events = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    init {
        viewModelScope.launch {
            withTimeoutOrNull(4000) { library.songsById.first { it.isNotEmpty() } }
            val s = library.song(songId)
            _song.value = s
            if (s != null) {
                val loaded = repo.load(s)
                text.value = loaded.raw
                _source.value = loaded.source
            }
            _ready.value = true
        }
    }

    fun importBytes(bytes: ByteArray) {
        text.value = repo.decode(bytes)
    }

    fun save(onDone: () -> Unit) {
        val s = _song.value ?: return
        viewModelScope.launch {
            if (text.value.isBlank()) repo.deleteSaved(s.id) else repo.save(s.id, text.value, LyricsSource.USER)
            events.emit(R.string.lyrics_saved)
            onDone()
        }
    }

    fun deleteSaved(onDone: () -> Unit) {
        val s = _song.value ?: return
        viewModelScope.launch {
            repo.deleteSaved(s.id)
            onDone()
        }
    }

    fun isPlayingThisSong(): Boolean = player.state.value.song?.id == songId

    fun playThisSong() {
        _song.value?.let { player.play(listOf(it)) }
    }

    fun startSync() {
        val parsed = LrcParser.parse(text.value)
        val lines = parsed.lines.filter { it.text.isNotBlank() }.map { SyncLine(it.text, if (parsed.synced) it.timeMs else null) }
        if (lines.isEmpty()) {
            events.tryEmit(R.string.lyrics_sync_needs_text)
            return
        }
        _sync.value = lines.map { it.copy(timeMs = null) }
        _syncIndex.value = 0
        if (!isPlayingThisSong()) playThisSong() else player.seekTo(0)
    }

    /** Stamps the current line with the playback position and moves to the next one. */
    fun mark() {
        val lines = _sync.value ?: return
        val i = _syncIndex.value
        if (i >= lines.size) return
        _sync.value = lines.toMutableList().also { it[i] = it[i].copy(timeMs = player.position) }
        _syncIndex.value = i + 1
    }

    fun undoMark() {
        val lines = _sync.value ?: return
        val i = (_syncIndex.value - 1).coerceAtLeast(0)
        _sync.value = lines.toMutableList().also { it[i] = it[i].copy(timeMs = null) }
        _syncIndex.value = i
        val previous = lines.getOrNull(i - 1)?.timeMs ?: 0
        player.seekTo((previous - 1500).coerceAtLeast(0))
    }

    fun finishSync(apply: Boolean) {
        val lines = _sync.value
        if (apply && lines != null) {
            val timed = lines.filter { it.timeMs != null }.map { LyricLine(it.timeMs!!, it.text) }
            if (timed.isNotEmpty()) text.value = LrcParser.toLrc(timed).trim()
        }
        _sync.value = null
    }
}
