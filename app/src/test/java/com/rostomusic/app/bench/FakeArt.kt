package com.rostomusic.app.bench

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

/**
 * Pictures for the bench: every album and artist photo is a flat colour with its name written on it,
 * so each frame shows which picture is on screen. Small pictures (lists) take 30 ms, large ones
 * (banners) 150 ms, like reading and decoding on a mid-range phone.
 */
class FakeArtFetcher(private val data: Uri, private val options: Options, private val context: Context) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val w = (options.size.width as? Dimension.Pixels)?.px ?: 400
        val h = (options.size.height as? Dimension.Pixels)?.px ?: 400
        val large = maxOf(w, h) > 300
        delay(if (large) 150 else 30)
        val (label, color) = describe(data)
        val bw = w.coerceIn(16, 900)
        val bh = h.coerceIn(16, 900)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(color)
        // A darker lower band, so the page colour taken from the lower edge differs per picture.
        val band = Paint().apply { this.color = darker(color) }
        canvas.drawRect(0f, bh * 0.75f, bw.toFloat(), bh.toFloat(), band)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = bh / 9f
            isFakeBoldText = true
        }
        canvas.drawText(label, bw * 0.06f, bh * 0.45f, p)
        return DrawableResult(BitmapDrawable(context.resources, bmp), isSampled = false, dataSource = DataSource.DISK)
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher = FakeArtFetcher(data, options, context)
    }

    companion object {
        fun describe(uri: Uri): Pair<String, Int> {
            val s = uri.toString()
            return when {
                s.contains("eminem") -> "PHOTO EMINEM" to Color.rgb(70, 45, 20)
                s.contains("lionhill") -> "PHOTO LION" to Color.rgb(150, 60, 110)
                else -> {
                    val album = uri.getQueryParameter("a")?.toLongOrNull() ?: uri.lastPathSegment?.toLongOrNull() ?: 0L
                    val palette = listOf(
                        Color.rgb(200, 160, 20), Color.rgb(30, 90, 160), Color.rgb(160, 40, 40),
                        Color.rgb(40, 130, 80), Color.rgb(230, 230, 230), Color.rgb(90, 60, 150),
                    )
                    "ALBUM $album" to palette[(album % palette.size).toInt()]
                }
            }
        }

        // A white cover keeps a light lower edge (a light page, like Extreme), the others get darker.
        private fun darker(c: Int) = if (Color.red(c) > 200 && Color.green(c) > 200) Color.rgb(225, 225, 225)
            else Color.rgb(Color.red(c) * 6 / 10, Color.green(c) * 6 / 10, Color.blue(c) * 6 / 10)
    }
}

/**
 * Fake Internet for Deezer and Last.fm: answers after [delayMs], like a phone on mobile data.
 * Lion Hill has a photo and a biography; any other artist has nothing.
 */
object FakeNet : URLStreamHandlerFactory {
    @Volatile var delayMs = 200L
    /** Lion Hill's answers wait until the test opens the gate (a very slow network). */
    @Volatile var gate = java.util.concurrent.CountDownLatch(1)
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        runCatching { URL.setURLStreamHandlerFactory(this) }
    }

    override fun createURLStreamHandler(protocol: String): URLStreamHandler? = if (protocol != "https") null else object : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection = object : HttpURLConnection(u) {
            private val body: String by lazy {
                val q = u.toString()
                if (q.contains("Lion")) gate.await(20, java.util.concurrent.TimeUnit.SECONDS)
                Thread.sleep(delayMs)
                when {
                    q.contains("deezer") && q.contains("Lion") ->
                        """{"data":[{"name":"Lion Hill","picture_xl":"https://img.test/lionhill.jpg"}]}"""
                    q.contains("deezer") -> """{"data":[]}"""
                    q.contains("audioscrobbler") && q.contains("Lion") ->
                        """{"artist":{"url":"https://last.fm/lionhill","bio":{"content":"Lion Hill est une chanteuse malgache. Sa voix et ses textes en malgache l'ont fait connaitre dans tout le pays, et ses chansons passent a la radio depuis des annees."}}}"""
                    else -> """{"error":6}"""
                }
            }
            override fun connect() {}
            override fun disconnect() {}
            override fun usingProxy() = false
            override fun getResponseCode(): Int { body; return 200 }
            override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray())
        }
    }
}
