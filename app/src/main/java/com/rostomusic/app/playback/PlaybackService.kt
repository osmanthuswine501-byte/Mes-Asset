package com.rostomusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rostomusic.app.MainActivity
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.settings.Settings
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.data.tags.TagEditor
import com.rostomusic.app.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.pow

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {
    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var effects: AudioEffects
    @Inject lateinit var sleepTimer: SleepTimer
    @Inject lateinit var tags: TagEditor
    @Inject lateinit var bus: PlaybackBus
    @Inject lateinit var audioRoute: AudioRoute

    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var settings = Settings()
    private var gainFactor = 1f
    private var gainJob: Job? = null
    private var trackingId: Long? = null
    private var listenedMs = 0L
    private var lastTick = 0L
    private var lastSave = 0L
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        // The FFmpeg decoder is only used when the phone has no decoder for a format, e.g. Dolby E-AC-3.
        val renderers = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.audioSessionId = Util.generateAudioSessionIdV21(this)
        effects.attach(player.audioSessionId, settings)
        player.addListener(listener)

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaLibrarySession.Builder(this, player, callback).setSessionActivity(open).setBitmapLoader(SongArtBitmapLoader(this)).build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_music) }
        )

        // The output picked in the app's device sheet; null lets Android route as usual.
        scope.launch { audioRoute.preferred.collect { runCatching { player.setPreferredAudioDevice(it?.device) } } }

        scope.launch {
            var first = true
            settingsStore.flow.collect { s ->
                val gainChanged = s.replayGain != settings.replayGain || s.preampDb != settings.preampDb
                settings = s
                effects.apply(s)
                if (first) {
                    player.setPlaybackSpeed(s.speed)
                    first = false
                }
                if (gainChanged) updateGain()
            }
        }
        scope.launch {
            lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(200)
                tick()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastTick
        lastTick = now
        if (player.isPlaying) listenedMs += elapsed

        when (val sleep = sleepTimer.state.value) {
            is SleepState.Until -> if (now >= sleep.endAt) {
                player.pause()
                sleepTimer.cancel()
            }
            else -> Unit
        }

        // Fade in at the start and out at the end of each track when the option is on.
        var factor = 1f
        val fadeMs = settings.fadeSec * 1000L
        val duration = player.duration
        if (fadeMs > 0 && duration != C.TIME_UNSET && duration > fadeMs * 3) {
            val pos = player.currentPosition
            val remaining = duration - pos
            factor = when {
                remaining < fadeMs -> (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
                pos < fadeMs -> (pos.toFloat() / fadeMs).coerceIn(0.05f, 1f)
                else -> 1f
            }
        }
        val target = (gainFactor * factor).coerceIn(0f, 1f)
        if (kotlin.math.abs(player.volume - target) > 0.01f) player.volume = target

        if (player.isPlaying && now - lastSave > 5000) {
            lastSave = now
            val index = player.currentMediaItemIndex
            val pos = player.currentPosition
            scope.launch { settingsStore.savePosition(index, pos) }
        }
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListen()
            trackingId = mediaItem?.mediaId?.toLongOrNull()
            updateGain()
            if (sleepTimer.state.value == SleepState.EndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                player.pause()
                sleepTimer.cancel()
            }
            saveQueue()
            updateWidget()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) saveQueue()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveErrors = 0
            else {
                val index = player.currentMediaItemIndex
                val pos = player.currentPosition
                scope.launch { settingsStore.savePosition(index, pos) }
            }
            updateWidget()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                flushListen()
                if (sleepTimer.state.value == SleepState.EndOfTrack) sleepTimer.cancel()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val title = player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            bus.reportError(title)
            consecutiveErrors++
            // Skip the file that cannot be decoded, but stop after a few failures in a row.
            if (player.hasNextMediaItem() && consecutiveErrors < 4) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    private fun flushListen() {
        val id = trackingId
        val listened = listenedMs
        listenedMs = 0
        if (id == null || listened < 5000) return
        val duration = library.song(id)?.duration ?: 0
        val counted = listened >= minOf(30_000L, (duration / 2).coerceAtLeast(5000))
        scope.launch { library.recordPlay(id, listened, counted) }
    }

    private fun updateGain() {
        gainJob?.cancel()
        val preamp = settings.preampDb
        if (!settings.replayGain) {
            gainFactor = dbToFactor(preamp)
            return
        }
        val song = trackingId?.let { library.song(it) }
        if (song == null) {
            gainFactor = dbToFactor(preamp)
            return
        }
        gainJob = scope.launch {
            val gain = runCatching { tags.readReplayGain(song) }.getOrNull() ?: 0f
            gainFactor = dbToFactor(gain + preamp)
        }
    }

    /** The player can only attenuate, so positive gains are capped at full volume. */
    private fun dbToFactor(db: Float): Float = 10f.pow(db / 20f).coerceIn(0.05f, 1f)

    private fun saveQueue() {
        val count = player.mediaItemCount
        val ids = (0 until minOf(count, 3000)).joinToString(",") { player.getMediaItemAt(it).mediaId }
        val index = player.currentMediaItemIndex
        val pos = player.currentPosition
        scope.launch { settingsStore.saveQueue(ids, index, pos) }
    }

    private fun updateWidget() {
        val item = player.currentMediaItem
        val song = item?.mediaId?.toLongOrNull()?.let { library.song(it) }
        WidgetUpdater.update(
            this,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            playing = player.isPlaying,
            art = song?.artUri?.toString().orEmpty(),
        )
    }

    private fun unknownArtist() = getString(R.string.unknown_artist)

    private fun resolve(item: MediaItem): MediaItem {
        if (item.localConfiguration != null) return item
        val song = item.mediaId.toLongOrNull()?.let { library.song(it) }
        return song?.toMediaItem(unknownArtist()) ?: item
    }

    private val callback = object : MediaLibrarySession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.map { resolve(it) }.toMutableList())

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            withTimeoutOrNull(4000) { library.songsById.first { it.isNotEmpty() } }
            val s = settingsStore.current()
            val songs = library.songsOf(s.lastQueue.split(',').mapNotNull { it.toLongOrNull() })
            if (songs.isEmpty()) throw IllegalStateException("nothing to resume")
            MediaSession.MediaItemsWithStartPosition(
                songs.map { it.toMediaItem(unknownArtist()) },
                s.lastIndex.coerceIn(0, songs.size - 1),
                s.lastPosition,
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(browsableItem(ROOT, getString(R.string.app_name)), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val song = mediaId.toLongOrNull()?.let { library.song(it) }
            return Futures.immediateFuture(
                if (song != null) LibraryResult.ofItem(song.toMediaItem(unknownArtist()), null)
                else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val all = children(parentId)
            val from = (page * pageSize).coerceIn(0, all.size)
            val to = (from + pageSize).coerceIn(from, all.size)
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, to)), params))
        }
    }

    /** Browse tree shown by Android Auto and other media browsers. */
    private fun children(parentId: String): List<MediaItem> {
        fun songs(list: List<Song>) = list.take(500).map { it.toMediaItem(unknownArtist()) }
        return when {
            parentId == ROOT -> listOf(
                browsableItem("songs", getString(R.string.tab_songs)),
                browsableItem("albums", getString(R.string.tab_albums), mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsableItem("artists", getString(R.string.tab_artists), mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsableItem("playlists", getString(R.string.tab_playlists), mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                browsableItem("favorites", getString(R.string.favorites)),
            )
            parentId == "songs" -> songs(library.songs.value)
            parentId == "favorites" -> songs(library.songs.value.filter { it.favorite })
            parentId == "albums" -> library.albums.value.map {
                browsableItem("album:${it.id}", it.title.ifEmpty { getString(R.string.unknown_album) }, it.artist, MediaMetadata.MEDIA_TYPE_ALBUM, Song.albumArtUri(it.id))
            }
            parentId == "artists" -> library.artists.value.map {
                browsableItem("artist:${it.name}", it.name.ifEmpty { unknownArtist() }, null, MediaMetadata.MEDIA_TYPE_ARTIST)
            }
            parentId == "playlists" -> library.playlists.value.map {
                browsableItem("playlist:${it.id}", it.name, null, MediaMetadata.MEDIA_TYPE_PLAYLIST)
            }
            parentId.startsWith("album:") -> {
                val id = parentId.removePrefix("album:").toLongOrNull()
                songs(library.songs.value.filter { it.albumId == id }.sortedWith(compareBy({ it.disc }, { it.track })))
            }
            parentId.startsWith("artist:") -> {
                val name = parentId.removePrefix("artist:")
                songs(library.songs.value.filter { it.artist == name })
            }
            parentId.startsWith("playlist:") -> {
                val id = parentId.removePrefix("playlist:").toLongOrNull()
                songs(library.songsOf(library.playlists.value.firstOrNull { it.id == id }?.songIds.orEmpty()))
            }
            else -> emptyList()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        flushListen()
        effects.release()
        session?.release()
        session = null
        player.removeListener(listener)
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ROOT = "root"
    }
}
