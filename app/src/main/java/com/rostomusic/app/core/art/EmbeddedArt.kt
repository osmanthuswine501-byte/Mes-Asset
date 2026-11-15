package com.rostomusic.app.core.art

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.rostomusic.app.data.db.Song
import okio.Buffer

/** Which file holds the picture of each album, for albums whose art Android did not extract. */
object ArtRegistry {
    @Volatile
    private var byAlbum: Map<Long, Uri> = emptyMap()

    fun update(songs: List<Song>) {
        val map = HashMap<Long, Uri>()
        for (s in songs) if (s.albumId !in map) map[s.albumId] = s.uri
        byAlbum = map
    }

    fun fileFor(albumId: Long): Uri? = byAlbum[albumId]
}

/**
 * Loads `rostoart://album/<id>`, the picture embedded in the album's first file, and
 * `rostoart://song/<id>?f=<file>&a=<album>`, the picture inside that song's own file, else its album's art.
 */
class EmbeddedArtFetcher(private val uri: Uri, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val bytes = if (uri.authority == "song") songPicture(options.context, uri) else {
            val albumId = uri.lastPathSegment?.toLongOrNull() ?: return null
            val file = ArtRegistry.fileFor(albumId) ?: return null
            readPicture(options.context, file)
        } ?: return null
        return SourceResult(
            source = ImageSource(Buffer().write(bytes), options.context),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "rostoart") EmbeddedArtFetcher(data, options) else null
    }

    companion object {
        private val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")

        /** The picture of one song, for a `rostoart://song/…` address. */
        fun songPicture(context: Context, uri: Uri): ByteArray? {
            val file = uri.getQueryParameter("f")?.let(Uri::parse) ?: return null
            readPicture(context, file)?.let { return it }
            val albumId = uri.getQueryParameter("a")?.toLongOrNull() ?: return null
            return runCatching {
                context.contentResolver.openInputStream(android.content.ContentUris.withAppendedId(ALBUM_ART, albumId))?.use { it.readBytes() }
            }.getOrNull()
        }

        fun readPicture(context: Context, file: Uri): ByteArray? {
            val mmr = MediaMetadataRetriever()
            return try {
                if (file.scheme == "file") mmr.setDataSource(file.path) else mmr.setDataSource(context, file)
                mmr.embeddedPicture
            } catch (e: Throwable) {
                null
            } finally {
                runCatching { mmr.release() }
            }
        }
    }
}
