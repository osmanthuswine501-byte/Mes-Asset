package com.rostomusic.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.domain.QueueOps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val queue: List<Song> = emptyList(),
    val index: Int = -1,
    val repeat: QueueOps.Repeat = QueueOps.Repeat.OFF,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
    val duration: Long = 0,
)

/** Messages from the playback service to the screens. */
@Singleton
class PlaybackBus @Inject constructor() {
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors
    fun reportError(title: String) {
        _errors.tryEmit(title)
    }
}

/** The app's single entry point to playback: wraps the media controller bound to [PlaybackService]. */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var originalOrder: List<String>? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val unknownArtist: String get() = context.getString(R.string.unknown_artist)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(events.contains(Player.EVENT_TIMELINE_CHANGED))
        }

        override fun onPlayerError(error: PlaybackException) = Unit
    }

    init {
        scope.launch { library.songsById.collect { sync(true) } }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull()
            if (c == null) {
                future = null
                return@addListener
            }
            controller = c
            c.addListener(listener)
            sync(true)
            val actions = ArrayList(pending)
            pending.clear()
            if (actions.isEmpty()) restoreLastQueue(c) else actions.forEach { it(c) }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun restoreLastQueue(c: MediaController) {
        if (c.mediaItemCount > 0) return
        scope.launch {
            val s = settings.current()
            if (!s.resumeOnStart || s.lastQueue.isEmpty()) return@launch
            withTimeoutOrNull(5000) { library.songsById.first { it.isNotEmpty() } } ?: return@launch
            val songs = library.songsOf(s.lastQueue.split(',').mapNotNull { it.toLongOrNull() })
            if (songs.isEmpty() || c.mediaItemCount > 0) return@launch
            c.setMediaItems(songs.map { it.toMediaItem(unknownArtist) }, s.lastIndex.coerceIn(0, songs.size - 1), s.lastPosition)
            c.prepare()
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c)
        else {
            pending += block
            connect()
        }
    }

    private fun sync(rebuildQueue: Boolean) {
        val c = controller ?: return
        val old = _state.value
        val count = c.mediaItemCount
        val queue = if (rebuildQueue || old.queue.size != count) {
            val map = library.songsById.value
            (0 until count).map { i ->
                val item = c.getMediaItemAt(i)
                map[item.mediaId.toLongOrNull()] ?: placeholder(item)
            }
        } else old.queue
        val index = if (count == 0) -1 else c.currentMediaItemIndex
        val song = queue.getOrNull(index)
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: song?.duration ?: 0
        _state.value = old.copy(
            song = song,
            isPlaying = c.isPlaying,
            queue = queue,
            index = index,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> QueueOps.Repeat.ALL
                Player.REPEAT_MODE_ONE -> QueueOps.Repeat.ONE
                else -> QueueOps.Repeat.OFF
            },
            shuffle = originalOrder != null && count > 0,
            speed = c.playbackParameters.speed,
            duration = duration,
        )
    }

    /** Stands in for a queued file that is no longer in the library. */
    private fun placeholder(item: MediaItem): Song = Song(
        id = item.mediaId.toLongOrNull() ?: -1, title = item.mediaMetadata.title?.toString().orEmpty(),
        artist = item.mediaMetadata.artist?.toString().orEmpty(), album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
        albumArtist = "", albumId = -1, genre = "", year = 0, track = 0, disc = 0, duration = 0, size = 0, path = "",
        folder = "", mime = "", bitrate = 0, composer = "", dateAdded = 0, dateModified = 0,
    )

    val position: Long get() = controller?.currentPosition ?: 0

    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        withController { c ->
            val start = startIndex.coerceIn(0, songs.size - 1)
            if (shuffle) {
                originalOrder = songs.map { it.id.toString() }
                val order = QueueOps.shuffledKeepingFirst(songs, start)
                c.setMediaItems(order.map { it.toMediaItem(unknownArtist) }, 0, 0)
            } else {
                originalOrder = null
                c.setMediaItems(songs.map { it.toMediaItem(unknownArtist) }, start, 0)
            }
            c.prepare()
            c.play()
        }
    }

    /** Plays a random order of [songs] starting anywhere. */
    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        play(songs, songs.indices.random(), shuffle = true)
    }

    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { c ->
            val empty = c.mediaItemCount == 0
            val at = if (empty) 0 else c.currentMediaItemIndex + 1
            c.addMediaItems(at, songs.map { it.toMediaItem(unknownArtist) })
            if (empty) {
                c.prepare()
                c.play()
            }
        }
    }

    fun enqueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { c ->
            val empty = c.mediaItemCount == 0
            c.addMediaItems(songs.map { it.toMediaItem(unknownArtist) })
            if (empty) {
                c.prepare()
                c.play()
            }
        }
    }

    fun toggle() = withController { c ->
        if (c.isPlaying) c.pause()
        else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0)
            c.play()
        }
    }

    fun pause() = withController { it.pause() }
    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0)) }
    fun seekBy(deltaMs: Long) = withController { c ->
        val max = c.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, max))
    }

    fun skipTo(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (QueueOps.nextRepeat(_state.value.repeat)) {
            QueueOps.Repeat.OFF -> Player.REPEAT_MODE_OFF
            QueueOps.Repeat.ALL -> Player.REPEAT_MODE_ALL
            QueueOps.Repeat.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    /** Shuffling reorders the real queue around the current track, so playback never restarts. */
    fun toggleShuffle() = withController { c ->
        val count = c.mediaItemCount
        if (count == 0) return@withController
        val cur = c.currentMediaItemIndex
        val items = (0 until count).map { c.getMediaItemAt(it) }
        val original = originalOrder
        val reordered: List<MediaItem>
        val newIndex: Int
        if (original == null) {
            originalOrder = items.map { it.mediaId }
            reordered = QueueOps.shuffledKeepingFirst(items, cur)
            newIndex = 0
        } else {
            val restoredIds = QueueOps.restoreOrder(items.map { it.mediaId }, original)
            val pool = HashMap<String, ArrayDeque<MediaItem>>()
            items.forEachIndexed { i, item -> if (i != cur) pool.getOrPut(item.mediaId) { ArrayDeque() }.addLast(item) }
            val curId = items[cur].mediaId
            var placed = false
            var idx = 0
            val out = ArrayList<MediaItem>(count)
            for (id in restoredIds) {
                val fromPool = pool[id]?.removeFirstOrNull()
                if (fromPool != null) out += fromPool
                else if (id == curId && !placed) {
                    placed = true
                    idx = out.size
                    out += items[cur]
                }
            }
            if (!placed) {
                idx = 0
                out.add(0, items[cur])
            }
            originalOrder = null
            reordered = out
            newIndex = idx
        }
        if (cur + 1 < count) c.removeMediaItems(cur + 1, count)
        if (cur > 0) c.removeMediaItems(0, cur)
        if (newIndex > 0) c.addMediaItems(0, reordered.subList(0, newIndex))
        if (newIndex + 1 < reordered.size) c.addMediaItems(reordered.subList(newIndex + 1, reordered.size))
        sync(true)
    }

    fun setSpeed(speed: Float) = withController { c ->
        c.setPlaybackSpeed(speed)
        scope.launch { settings.setSpeed(speed) }
    }

    fun move(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    fun removeAt(index: Int) = withController { c -> if (index in 0 until c.mediaItemCount) c.removeMediaItem(index) }

    fun clearQueue() = withController { c ->
        originalOrder = null
        c.stop()
        c.clearMediaItems()
    }
}
