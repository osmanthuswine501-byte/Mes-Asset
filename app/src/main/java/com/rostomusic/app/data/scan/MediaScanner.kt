package com.rostomusic.app.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.domain.Mp4Duration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MediaScanner @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Reads every audio file of the phone. A deep scan also walks the storage, asks Android to index
     * the audio files it missed, and reads the ones it still ignores straight from the file.
     * [fileOnlyKnown] are files found that way by an earlier deep scan; a quick scan keeps them.
     */
    suspend fun scan(
        minDurationSec: Int,
        excludedFolders: Set<String>,
        deep: Boolean,
        fileOnlyKnown: List<Song>,
        onProgress: (Int) -> Unit,
    ): List<Song> = withContext(Dispatchers.IO) {
        fun isExcluded(folder: String) = excludedFolders.any { folder == it || folder.startsWith("$it/") }

        var indexed = queryMediaStore(onProgress)
        val fileOnly: List<Song>
        if (deep) {
            val onDisk = walkStorage(::isExcluded)
            var missing = onDisk.filter { it.absolutePath !in indexed }
            if (missing.isNotEmpty()) {
                requestIndex(missing.map { it.absolutePath })
                indexed = queryMediaStore(onProgress)
                missing = missing.filter { it.absolutePath !in indexed }
            }
            fileOnly = missing.mapNotNull { readFile(it) }
        } else {
            fileOnly = fileOnlyKnown.filter { it.path !in indexed && File(it.path).exists() }
        }
        // Android reports a zero duration for some files, like fragmented MP4 from download tools.
        val all = indexed.values.map { s -> if (s.duration > 0) s else s.copy(duration = measureDuration(s.path)) } + fileOnly
        // A file whose length nobody could read is kept when it is big enough to be a song, not a short sound.
        all.filter { (it.duration >= minDurationSec * 1000L || (it.duration <= 0L && it.size >= MIN_UNKNOWN_SIZE)) && !isExcluded(it.folder) }
            .also { onProgress(it.size) }
    }

    /** Every audio entry of the media store except ringtones, notifications and alarms, keyed by path. */
    private fun queryMediaStore(onProgress: (Int) -> Unit): Map<String, Song> {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add("duration")
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.COMPOSER)
            if (modern) {
                add("album_artist")
                add("genre")
                add("bitrate")
                add("disc_number")
                add("cd_track_number")
            }
        }.toTypedArray()

        val genreById = if (modern) emptyMap() else legacyGenres()
        val out = LinkedHashMap<String, Song>()
        // Everything audio, not only what Android flags as music: downloaded files are often not flagged.
        val selection = "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0"
        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = c.getColumnIndexOrThrow("duration")
            val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iModified = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val iComposer = c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
            val iAlbumArtist = c.getColumnIndex("album_artist")
            val iGenre = c.getColumnIndex("genre")
            val iBitrate = c.getColumnIndex("bitrate")
            val iDisc = c.getColumnIndex("disc_number")
            val iCdTrack = c.getColumnIndex("cd_track_number")

            while (c.moveToNext()) {
                val path = c.getString(iData) ?: continue
                val duration = c.getLong(iDuration)
                val folder = path.substringBeforeLast('/', "")
                val id = c.getLong(iId)
                val rawTrack = c.getInt(iTrack)
                val disc = if (iDisc >= 0) (c.getString(iDisc)?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0)
                else rawTrack / 1000
                val track = if (iCdTrack >= 0) (c.getString(iCdTrack)?.substringBefore('/')?.trim()?.toIntOrNull() ?: rawTrack % 1000)
                else rawTrack % 1000
                out[path] = Song(
                    id = id,
                    title = c.getString(iTitle).orEmpty().ifBlank { path.substringAfterLast('/').substringBeforeLast('.') },
                    artist = clean(c.getString(iArtist)),
                    album = clean(c.getString(iAlbum)),
                    albumArtist = if (iAlbumArtist >= 0) clean(c.getString(iAlbumArtist)) else "",
                    albumId = c.getLong(iAlbumId),
                    genre = if (iGenre >= 0) clean(c.getString(iGenre)) else genreById[id].orEmpty(),
                    year = c.getInt(iYear),
                    track = track,
                    disc = disc.coerceAtLeast(0),
                    duration = duration,
                    size = c.getLong(iSize),
                    path = path,
                    folder = folder,
                    mime = c.getString(iMime).orEmpty(),
                    bitrate = if (iBitrate >= 0) c.getInt(iBitrate) else estimateBitrate(c.getLong(iSize), duration),
                    composer = clean(c.getString(iComposer)),
                    dateAdded = c.getLong(iAdded) * 1000,
                    dateModified = c.getLong(iModified) * 1000,
                )
                if (out.size % 50 == 0) onProgress(out.size)
            }
        }
        return out
    }

    /** Audio files on the internal storage, skipping system and hidden folders. */
    private fun walkStorage(isExcluded: (String) -> Boolean): List<File> {
        val root = Environment.getExternalStorageDirectory() ?: return emptyList()
        val out = ArrayList<File>()
        val pending = ArrayDeque<Pair<File, Int>>()
        pending += root to 0
        var visited = 0
        while (pending.isNotEmpty() && out.size < MAX_FILES && visited < MAX_DIRS) {
            val (dir, depth) = pending.removeFirst()
            visited++
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (f in children) {
                val name = f.name
                if (name.startsWith('.')) continue
                if (f.isDirectory) {
                    if (depth == 0 && name.equals("Android", ignoreCase = true)) continue
                    if (depth < MAX_DEPTH && !isExcluded(f.absolutePath)) pending += f to depth + 1
                } else if (name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS && f.length() > 0) {
                    out += f
                }
            }
        }
        return out
    }

    /** Asks Android to index files it missed, and waits a little for it to finish. */
    private suspend fun requestIndex(paths: List<String>) {
        val left = AtomicInteger(paths.size)
        withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { cont ->
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _: String?, _: Uri? ->
                    if (left.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    /** Builds a song from the file itself, for the rare files Android refuses to index as audio. */
    private fun readFile(file: File): Song? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            fun meta(key: Int) = mmr.extractMetadata(key)?.trim().orEmpty()
            if (meta(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO).isNotEmpty() && meta(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != "yes") return null
            val path = file.absolutePath
            val folder = path.substringBeforeLast('/', "")
            val album = meta(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = meta(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0L
            Song(
                id = stableNegativeId(path),
                title = meta(MediaMetadataRetriever.METADATA_KEY_TITLE).ifEmpty { file.nameWithoutExtension },
                artist = meta(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = album,
                albumArtist = meta(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                albumId = stableNegativeId("$folder|$album"),
                genre = meta(MediaMetadataRetriever.METADATA_KEY_GENRE),
                year = meta(MediaMetadataRetriever.METADATA_KEY_YEAR).take(4).toIntOrNull() ?: 0,
                track = meta(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).substringBefore('/').toIntOrNull() ?: 0,
                disc = meta(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).substringBefore('/').toIntOrNull() ?: 0,
                duration = if (duration > 0) duration else measureDuration(path),
                size = file.length(),
                path = path,
                folder = folder,
                mime = meta(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                bitrate = meta(MediaMetadataRetriever.METADATA_KEY_BITRATE).toIntOrNull() ?: estimateBitrate(file.length(), duration),
                composer = meta(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                dateAdded = file.lastModified(),
                dateModified = file.lastModified(),
                contentUri = Uri.fromFile(file).toString(),
            )
        } catch (e: Throwable) {
            // Android cannot open the file (e.g. Dolby Atmos M4A): keep it with what the name and boxes say.
            val path = file.absolutePath
            val folder = path.substringBeforeLast('/', "")
            val duration = measureDuration(path)
            Song(
                id = stableNegativeId(path),
                title = file.nameWithoutExtension,
                artist = "",
                album = "",
                albumArtist = "",
                albumId = stableNegativeId("$folder|"),
                genre = "",
                year = 0,
                track = 0,
                disc = 0,
                duration = duration,
                size = file.length(),
                path = path,
                folder = folder,
                mime = if (file.extension.lowercase() in MP4_EXTENSIONS) "audio/mp4" else "",
                bitrate = estimateBitrate(file.length(), duration),
                composer = "",
                dateAdded = file.lastModified(),
                dateModified = file.lastModified(),
                contentUri = Uri.fromFile(file).toString(),
            )
        } finally {
            runCatching { mmr.release() }
        }
    }

    /**
     * Duration read by the platform demuxer, used when the index says zero. Android cannot read some
     * MP4 files (fragmented Tidal / Dolby Atmos M4A), so their boxes are read directly.
     */
    private fun measureDuration(path: String): Long {
        val mmr = MediaMetadataRetriever()
        val platform = try {
            mmr.setDataSource(path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Throwable) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
        if (platform > 0) return platform
        val ext = path.substringAfterLast('.', "").lowercase()
        return if (ext in MP4_EXTENSIONS) Mp4Duration.millis(File(path)) else 0L
    }

    private fun stableNegativeId(key: String): Long {
        val crc = CRC32().apply { update(key.toByteArray()) }.value
        return -(((crc shl 20) xor (key.hashCode().toLong() and 0xFFFFF)) + 1)
    }

    private fun clean(value: String?): String {
        val v = value?.trim().orEmpty()
        return if (v == "<unknown>" || v == MediaStore.UNKNOWN_STRING) "" else v
    }

    private fun estimateBitrate(size: Long, durationMs: Long): Int =
        if (durationMs <= 0) 0 else (size * 8 * 1000 / durationMs).toInt()

    /** Before Android 11 the genre is not a column of the audio table, so it is read genre by genre. */
    private fun legacyGenres(): Map<Long, String> {
        val map = HashMap<Long, String>()
        runCatching {
            val genres = ArrayList<Pair<Long, String>>()
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1)?.trim().orEmpty()
                    if (name.isNotEmpty()) genres += c.getLong(0) to name
                }
            }
            for ((gid, name) in genres) {
                context.contentResolver.query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", gid),
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null,
                )?.use { c -> while (c.moveToNext()) map[c.getLong(0)] = name }
            }
        }
        return map
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "m4b", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "aiff", "aif", "alac",
            "mka", "ac3", "eac3", "ec3", "ape", "wv", "dsf", "amr", "3ga",
        )
        val MP4_EXTENSIONS = setOf("m4a", "m4b", "mp4", "alac", "aac")
        /** 1 MB: about a minute of compressed audio. */
        const val MIN_UNKNOWN_SIZE = 1_000_000L
        const val MAX_DEPTH = 10
        const val MAX_DIRS = 8_000
        const val MAX_FILES = 30_000
    }
}
