package com.rostomusic.app.data.db

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs", indices = [Index("albumId"), Index("path")])
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val albumId: Long,
    val genre: String,
    val year: Int,
    val track: Int,
    val disc: Int,
    val duration: Long,
    val size: Long,
    val path: String,
    val folder: String,
    val mime: String,
    val bitrate: Int,
    val composer: String,
    val dateAdded: Long,
    val dateModified: Long,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val listenedMs: Long = 0,
    /** Set for files read straight from storage because Android has not indexed them as audio. */
    @ColumnInfo(defaultValue = "") val contentUri: String = "",
) {
    val uri: Uri
        get() = if (contentUri.isNotEmpty()) Uri.parse(contentUri)
        else ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    val isFileOnly: Boolean get() = contentUri.startsWith("file:")
    /** This song's own cover: the picture inside its file, else its album's art when the album is a real one. */
    val artUri: Uri get() = songArtUri(this)

    /**
     * Android names an untagged file's album after its folder, so every untagged song of a folder shares
     * one album and one picture. Only a tagged album, not named like its folder, may lend its art to a song.
     */
    val hasRealAlbum: Boolean get() = album.isNotBlank() && !album.equals(folder.substringAfterLast('/'), ignoreCase = true)

    val extension: String get() = path.substringAfterLast('.', "").lowercase()

    /** Short format label shown as a badge, e.g. MP3 or FLAC. */
    val format: String
        get() = when {
            extension in KNOWN_EXT -> extension.uppercase()
            mime.contains("mpeg") -> "MP3"
            mime.contains("flac") -> "FLAC"
            mime.contains("mp4") || mime.contains("aac") -> "AAC"
            mime.contains("ogg") -> "OGG"
            mime.contains("opus") -> "OPUS"
            mime.contains("wav") -> "WAV"
            else -> extension.uppercase().ifEmpty { "AUDIO" }
        }

    val isLossless: Boolean get() = extension in LOSSLESS_EXT || mime.contains("flac") || mime.contains("wav")

    companion object {
        private val KNOWN_EXT = setOf("mp3", "flac", "wav", "ogg", "opus", "m4a", "aac", "aiff", "aif", "wma", "alac")
        private val LOSSLESS_EXT = setOf("flac", "wav", "aiff", "aif", "alac")
        private val ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
        /** Album art known to Android, or, for albums Android does not know, the picture inside the file. */
        fun albumArtUri(albumId: Long): Uri =
            if (albumId < 0) embeddedArtUri(albumId) else ContentUris.withAppendedId(ART_BASE, albumId)

        fun embeddedArtUri(albumId: Long): Uri = Uri.parse("rostoart://album/$albumId")

        fun songArtUri(song: Song): Uri = Uri.Builder().scheme("rostoart").authority("song").appendPath(song.id.toString())
            .appendQueryParameter("f", song.uri.toString())
            .appendQueryParameter("a", if (song.hasRealAlbum && song.albumId >= 0) song.albumId.toString() else "")
            .build()
    }
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs", indices = [Index("playlistId")])
data class PlaylistSong(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "history", indices = [Index("playedAt"), Index("songId")])
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
    val listenedMs: Long,
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    /** Raw LRC or plain text saved by the user or fetched online; empty when only an offset is stored. */
    val content: String,
    val source: String,
    val offsetMs: Long,
)

@Entity(tableName = "bookmarks", indices = [Index("songId")])
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val positionMs: Long,
    val label: String,
    val createdAt: Long,
)

@Entity(tableName = "waveforms")
data class WaveformEntity(
    @PrimaryKey val songId: Long,
    val bars: String,
)

/** Codec and bit depth of a file, read once in the background after a scan. */
@Entity(tableName = "song_quality")
data class SongQuality(
    @PrimaryKey val songId: Long,
    /** The file's modification time when it was read; a newer file is read again. */
    val dateModified: Long,
    val codecMime: String,
    val bitDepth: Int,
    /** Marked explicit in its tags: shows the "E" badge. */
    @ColumnInfo(defaultValue = "0") val explicit: Boolean = false,
)
