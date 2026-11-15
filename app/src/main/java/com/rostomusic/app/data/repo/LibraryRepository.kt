package com.rostomusic.app.data.repo

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.rostomusic.app.core.art.ArtRegistry
import com.rostomusic.app.data.db.HistoryEntry
import com.rostomusic.app.data.db.Playlist
import com.rostomusic.app.data.db.PlaylistSong
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.scan.MediaScanner
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.domain.QueueOps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class Album(val id: Long, val title: String, val artist: String, val year: Int, val songCount: Int, val duration: Long, val dateAdded: Long)
data class Artist(val name: String, val songCount: Int, val albumCount: Int, val albumId: Long)
data class Genre(val name: String, val songCount: Int, val albumId: Long)
data class Folder(val path: String, val songCount: Int, val albumId: Long) {
    val name: String get() = path.substringAfterLast('/').ifEmpty { path }
}
data class PlaylistInfo(val id: Long, val name: String, val createdAt: Long, val entries: List<PlaylistSong>) {
    val songIds: List<Long> get() = entries.map { it.songId }
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val count: Int) : ScanState
    data class Failed(val message: String) : ScanState
}

/** Identifies any list of songs that can be opened as a screen. */
object CollectionType {
    const val ALBUM = "album"
    const val ARTIST = "artist"
    const val GENRE = "genre"
    const val FOLDER = "folder"
    const val PLAYLIST = "playlist"
    const val FAVORITES = "favorites"
    const val RECENT_PLAYED = "recent_played"
    const val RECENT_ADDED = "recent_added"
    const val MOST_PLAYED = "most_played"
    const val NEVER_PLAYED = "never_played"
    const val DAILY_MIX = "daily_mix"
}

@Singleton
class LibraryRepository @Inject constructor(
    private val db: RostoDatabase,
    private val scanner: MediaScanner,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
    private val tags: com.rostomusic.app.data.tags.TagEditor,
) {
    /** Codec and bit depth per song id, filled in the background (see [fillQuality]). */
    val quality: StateFlow<Map<Long, com.rostomusic.app.data.db.SongQuality>> = db.quality().observeAll()
        .map { list -> list.associateBy { it.songId } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private var filling: kotlinx.coroutines.Job? = null

    /** Reads codec and bit depth of the songs not read yet, one at a time, without slowing the screens. */
    fun fillQuality() {
        if (filling?.isActive == true) return
        filling = scope.launch(Dispatchers.IO) {
            val known = db.quality().observeAll().first().associateBy { it.songId }
            for (s in songs.value) {
                val old = known[s.id]
                if (old != null && old.dateModified == s.dateModified) continue
                val q = runCatching { tags.readQuality(s) }.getOrNull()
                db.quality().put(com.rostomusic.app.data.db.SongQuality(s.id, s.dateModified, q?.codecMime.orEmpty(), q?.bits ?: 0, q?.explicit ?: false))
            }
        }
    }

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    val songs: StateFlow<List<Song>> = db.songs().observeAll()
        .onEach { _loaded.value = true }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val songsById: StateFlow<Map<Long, Song>> = songs
        .onEach { list -> ArtRegistry.update(list) }
        .map { list -> list.associateBy { it.id } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val albums: StateFlow<List<Album>> = songs.map { list ->
        list.groupBy { it.albumId }.map { (id, s) ->
            val first = s.first()
            Album(
                id = id,
                title = first.album,
                artist = s.firstOrNull { it.albumArtist.isNotEmpty() }?.albumArtist ?: mostCommon(s.map { it.artist }),
                year = s.maxOf { it.year },
                songCount = s.size,
                duration = s.sumOf { it.duration },
                dateAdded = s.maxOf { it.dateAdded },
            )
        }.sortedBy { it.title.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val artists: StateFlow<List<Artist>> = songs.map { list ->
        list.groupBy { it.artist }.map { (name, s) ->
            Artist(name, s.size, s.distinctBy { it.albumId }.size, s.first().albumId)
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val genres: StateFlow<List<Genre>> = songs.map { list ->
        list.filter { it.genre.isNotEmpty() }.groupBy { it.genre }.map { (name, s) -> Genre(name, s.size, s.first().albumId) }
            .sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<Folder>> = songs.map { list ->
        list.groupBy { it.folder }.map { (path, s) -> Folder(path, s.size, s.first().albumId) }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<PlaylistInfo>> =
        combine(db.playlists().observePlaylists(), db.playlists().observeEntries()) { lists, entries ->
            val byList = entries.groupBy { it.playlistId }
            lists.map { PlaylistInfo(it.id, it.name, it.createdAt, byList[it.id].orEmpty()) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scan
    private val scanMutex = Mutex()

    /**
     * [deep] also walks the storage for files Android has not indexed. It takes a few seconds,
     * so the automatic refresh after a change in the media store uses a quick scan.
     */
    suspend fun rescan(deep: Boolean = true) = scanMutex.withLock {
        _scan.value = ScanState.Scanning(0)
        try {
            val s = settings.current()
            val fileOnly = songs.value.filter { it.isFileOnly }
            val scanned = scanner.scan(s.minDurationSec, s.excludedFolders, deep, fileOnly) { _scan.value = ScanState.Scanning(it) }
            db.songs().replaceScanned(scanned)
            _scan.value = ScanState.Idle
            fillQuality()
        } catch (e: SecurityException) {
            _scan.value = ScanState.Failed("permission")
        } catch (e: Exception) {
            _scan.value = ScanState.Failed(e.message ?: "error")
        }
    }

    private var observing = false
    private var lastDeepScan = 0L

    /** Refreshes the library when Android's media index changes, e.g. after a download finishes. */
    fun watchMediaStore(context: Context) {
        if (observing) return
        observing = true
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes.tryEmit(Unit)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        }
        scope.launch {
            @OptIn(FlowPreview::class)
            changes.debounce(2_000).collect { rescan(deep = false) }
        }
    }

    /** Deep scan when the app comes back to the screen, at most every two minutes. */
    fun refreshOnReturn() {
        val now = System.currentTimeMillis()
        if (now - lastDeepScan < 120_000) return
        lastDeepScan = now
        scope.launch { rescan(deep = true) }
    }

    fun song(id: Long): Song? = songsById.value[id]

    fun songsOf(ids: List<Long>): List<Song> {
        val map = songsById.value
        return ids.mapNotNull { map[it] }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = db.songs().setFavorite(id, favorite)

    suspend fun recordPlay(songId: Long, listenedMs: Long, counted: Boolean) {
        val now = System.currentTimeMillis()
        db.songs().recordPlay(songId, now, listenedMs, if (counted) 1 else 0)
        db.history().insert(HistoryEntry(songId = songId, playedAt = now, listenedMs = listenedMs))
    }

    /** Songs of any collection, kept up to date. */
    fun collectionSongs(type: String, key: String): Flow<List<Song>> = when (type) {
        CollectionType.ALBUM, CollectionType.ARTIST -> songs.map { l -> sortedCollection(type, key, l) }
        CollectionType.GENRE -> songs.map { l -> l.filter { it.genre == key } }
        CollectionType.FOLDER -> songs.map { l -> l.filter { it.folder == key }.sortedBy { it.path.lowercase() } }
        CollectionType.PLAYLIST -> combine(playlists, songsById) { pl, map ->
            pl.firstOrNull { it.id == key.toLongOrNull() }?.songIds?.mapNotNull { map[it] }.orEmpty()
        }
        CollectionType.FAVORITES -> songs.map { l -> l.filter { it.favorite } }
        CollectionType.RECENT_PLAYED -> songs.map { l -> l.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(100) }
        CollectionType.RECENT_ADDED -> songs.map { l -> l.sortedByDescending { it.dateAdded }.take(100) }
        CollectionType.MOST_PLAYED -> songs.map { l -> l.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(100) }
        CollectionType.NEVER_PLAYED -> songs.map { l -> l.filter { it.playCount == 0 } }
        CollectionType.DAILY_MIX -> songs.map { l -> dailyMix(l) }
        else -> songs.map { emptyList() }
    }.flowOn(Dispatchers.Default)

    /** Album and artist songs in page order; shared by the flow and [collectionSnapshot] so both agree. */
    private fun sortedCollection(type: String, key: String, l: List<Song>): List<Song> = when (type) {
        CollectionType.ALBUM -> l.filter { it.albumId == key.toLongOrNull() }.sortedWith(compareBy({ it.disc }, { it.track }, { it.title.lowercase() }))
        else -> l.filter { it.artist == key }.sortedWith(compareBy({ it.album.lowercase() }, { it.disc }, { it.track }))
    }

    /** An album or artist page's songs right now, in the same order as [collectionSongs]. */
    fun collectionSnapshot(type: String, key: String): List<Song> = sortedCollection(type, key, songs.value)

    /** Songs of a collection right now, without waiting for a flow. */
    fun collectionNow(type: String, key: String): List<Song> {
        val all = songs.value
        return when (type) {
            CollectionType.ALBUM -> all.filter { it.albumId == key.toLongOrNull() }.sortedWith(compareBy({ it.disc }, { it.track }))
            CollectionType.ARTIST -> all.filter { it.artist == key }
            CollectionType.GENRE -> all.filter { it.genre == key }
            CollectionType.FOLDER -> all.filter { it.folder == key }
            CollectionType.PLAYLIST -> songsOf(playlists.value.firstOrNull { it.id == key.toLongOrNull() }?.songIds.orEmpty())
            CollectionType.FAVORITES -> all.filter { it.favorite }
            CollectionType.RECENT_PLAYED -> all.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(100)
            CollectionType.RECENT_ADDED -> all.sortedByDescending { it.dateAdded }.take(100)
            CollectionType.MOST_PLAYED -> all.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(100)
            CollectionType.NEVER_PLAYED -> all.filter { it.playCount == 0 }
            CollectionType.DAILY_MIX -> dailyMix(all)
            else -> emptyList()
        }
    }

    fun dailyMix(all: List<Song>): List<Song> =
        QueueOps.dailyMix(all.sortedBy { it.id }, System.currentTimeMillis() / 86_400_000L, 30)

    // Playlists

    suspend fun createPlaylist(name: String, songIds: List<Long> = emptyList()): Long {
        val id = db.playlists().insert(Playlist(name = name.trim(), createdAt = System.currentTimeMillis()))
        if (songIds.isNotEmpty()) db.playlists().append(id, songIds)
        return id
    }

    suspend fun renamePlaylist(id: Long, name: String) = db.playlists().rename(id, name.trim())
    suspend fun deletePlaylist(id: Long) = db.playlists().delete(id)
    suspend fun addToPlaylist(id: Long, songIds: List<Long>) = db.playlists().append(id, songIds)
    suspend fun removePlaylistEntry(entryId: Long) = db.playlists().deleteEntry(entryId)
    suspend fun setPlaylistSongs(id: Long, songIds: List<Long>) = db.playlists().replace(id, songIds)

    private fun mostCommon(values: List<String>): String =
        values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
}
