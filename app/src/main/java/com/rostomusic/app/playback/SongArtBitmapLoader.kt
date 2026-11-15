package com.rostomusic.app.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.rostomusic.app.core.art.EmbeddedArtFetcher
import java.util.concurrent.Executors

/** Lets the notification and the lock screen show each song's own cover (`rostoart://song/…`). */
@UnstableApi
class SongArtBitmapLoader(private val context: Context) : BitmapLoader {
    private val fallback = DataSourceBitmapLoader(context)
    private val executor: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (uri.scheme != "rostoart") return fallback.loadBitmap(uri)
        return executor.submit<Bitmap> {
            val bytes = EmbeddedArtFetcher.songPicture(context, uri) ?: throw IllegalStateException("No cover")
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IllegalStateException("Unreadable cover")
        }
    }
}
