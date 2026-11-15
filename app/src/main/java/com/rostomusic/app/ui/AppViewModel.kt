package com.rostomusic.app.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.settings.Settings
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.domain.HomeConfig
import com.rostomusic.app.playback.PlaybackBus
import com.rostomusic.app.playback.PlayerConnection
import com.rostomusic.app.playback.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

/** A song whose action menu is open. [playlistEntryId] is set when it was opened from a playlist. */
data class SongMenu(val song: Song, val playlistEntryId: Long? = null)

enum class PlayerPanel { NONE, LYRICS, QUEUE }

/** Activity-wide state and actions shared by every screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    val library: LibraryRepository,
    val player: PlayerConnection,
    val settingsStore: SettingsStore,
    val sleepTimer: SleepTimer,
    val audioRoute: com.rostomusic.app.playback.AudioRoute,
    bus: PlaybackBus,
    artistPrefetcher: com.rostomusic.app.data.repo.ArtistPrefetcher,
) : ViewModel() {
    /** Artist photos already fetched in the background, by artist name ("" = none), for the Artists grid. */
    val artistPhotos = artistPrefetcher.photos

    val settings: StateFlow<Settings?> = settingsStore.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val playerState = player.state

    val playerExpanded = MutableStateFlow(false)
    val playerPanel = MutableStateFlow(PlayerPanel.NONE)
    val songMenu = MutableStateFlow<SongMenu?>(null)
    val playlistPicker = MutableStateFlow<List<Song>?>(null)
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)

    /** Colour of the open Album, Artist or Song page, which the bottom bar and mini-player follow. */
    val pageTone = MutableStateFlow<Pair<Any, androidx.compose.ui.graphics.Color>?>(null)
    fun setPageTone(owner: Any, color: androidx.compose.ui.graphics.Color) { pageTone.value = owner to color }
    fun clearPageTone(owner: Any) { if (pageTone.value?.first === owner) pageTone.value = null }

    /** Colour behind the status bar while the full player is open (its cover's colour). */
    val playerTone = MutableStateFlow<androidx.compose.ui.graphics.Color?>(null)

    init {
        player.connect()
        viewModelScope.launch {
            bus.errors.collect { title -> messages.emit(UiMessage(R.string.unsupported_format, listOf(title))) }
        }
    }

    fun message(@StringRes res: Int, vararg args: Any) {
        messages.tryEmit(UiMessage(res, args.toList()))
    }

    fun rescan() {
        viewModelScope.launch { library.rescan() }
    }

    fun play(songs: List<Song>, index: Int = 0) = player.play(songs, index)
    fun shuffle(songs: List<Song>) = player.shufflePlay(songs)

    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        player.playNext(songs)
        message(R.string.msg_play_next)
    }

    fun enqueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        player.enqueue(songs)
        message(R.string.msg_added_to_queue, songs.size)
    }

    fun toggleFavorite(song: Song) {
        if (song.id < 0) return
        viewModelScope.launch { library.setFavorite(song.id, !song.favorite) }
    }

    fun addToPlaylist(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            library.addToPlaylist(playlistId, songs.map { it.id })
            message(R.string.msg_added_to_playlist)
        }
    }

    fun createPlaylist(name: String, songs: List<Song>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            library.createPlaylist(name, songs.map { it.id })
            message(R.string.msg_playlist_created)
        }
    }

    fun removePlaylistEntry(entryId: Long) {
        viewModelScope.launch { library.removePlaylistEntry(entryId) }
    }

    fun togglePin(pin: String, pinnedNow: Boolean) {
        viewModelScope.launch {
            settingsStore.updateHome { HomeConfig.togglePin(it, pin) }
            message(if (pinnedNow) R.string.msg_unpinned else R.string.msg_pinned)
        }
    }

    fun update(block: suspend SettingsStore.() -> Unit) {
        viewModelScope.launch { settingsStore.block() }
    }

    fun share(context: Context, song: Song) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = song.mime.ifEmpty { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, song.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun expandPlayer(panel: PlayerPanel = PlayerPanel.NONE) {
        playerPanel.value = panel
        playerExpanded.value = true
    }

    fun collapsePlayer() {
        playerExpanded.value = false
        playerPanel.value = PlayerPanel.NONE
    }
}

val LocalApp = staticCompositionLocalOf<AppViewModel> { error("AppViewModel missing") }
val LocalSettings = staticCompositionLocalOf { Settings() }

/** Space the floating bottom bar and mini player take, so lists can scroll clear of them. */
val LocalBottomInset = staticCompositionLocalOf<Dp> { 0.dp }

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library?tab={tab}"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection/{type}?key={key}"
    const val INFO = "info/{id}"
    const val LYRICS_EDITOR = "lyrics/{id}"
    const val EQUALIZER = "equalizer"
    const val STATS = "stats"
    const val CUSTOMIZE = "customize"
    const val ALBUMS = "albums"
    const val SONGS = "songs"
    const val ARTISTS = "artists"

    fun library(tab: Int = 0) = "library?tab=$tab"
    fun collection(type: String, key: String = "") = "collection/$type?key=${android.net.Uri.encode(key)}"
    fun info(id: Long) = "info/$id"
    fun lyricsEditor(id: Long) = "lyrics/$id"
}
