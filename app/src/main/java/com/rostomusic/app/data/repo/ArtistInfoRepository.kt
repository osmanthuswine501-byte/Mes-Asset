package com.rostomusic.app.data.repo

import android.content.Context
import com.rostomusic.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

data class ArtistInfo(
    val imageUrl: String,
    val bio: String,
    val bioUrl: String,
)

/**
 * Artist photo from Deezer and biography from Last.fm.
 * Results, including "not found", are cached on the phone so each artist is looked up rarely.
 */
@Singleton
class ArtistInfoRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir = File(context.filesDir, "artist_info").apply { mkdirs() }
    private val memory = java.util.concurrent.ConcurrentHashMap<String, ArtistInfo>()
    private val mutex = Mutex()

    /**
     * What is already known about [artist], without waiting: from memory, else from the small file
     * saved on the phone. Null when the artist was never looked up or has nothing.
     */
    fun peek(artist: String): ArtistInfo? {
        val name = artist.trim()
        if (name.isEmpty()) return null
        val info = memory[name] ?: readCache(File(dir, hash(name) + ".json"))?.also { memory[name] = it }
        return info?.takeIf { it.imageUrl.isNotEmpty() || it.bio.isNotEmpty() }
    }

    suspend fun get(artist: String): ArtistInfo? = withContext(Dispatchers.IO) {
        val name = artist.trim()
        if (name.isEmpty()) return@withContext null
        mutex.withLock { memory[name] }?.let { return@withContext it.takeIf { i -> i.imageUrl.isNotEmpty() || i.bio.isNotEmpty() } }
        val file = File(dir, hash(name) + ".json")
        readCache(file)?.let { cached ->
            mutex.withLock { memory[name] = cached }
            return@withContext cached.takeIf { it.imageUrl.isNotEmpty() || it.bio.isNotEmpty() }
        }
        val image = runCatching { deezerImage(name) }.getOrNull()
        val bio = runCatching { lastFmBio(name) }.getOrNull()
        // A network failure is not cached, so the next visit tries again.
        if (image == null && bio == null) return@withContext null
        val info = ArtistInfo(image.orEmpty(), bio?.first.orEmpty(), bio?.second.orEmpty())
        writeCache(file, info)
        mutex.withLock { memory[name] = info }
        info.takeIf { it.imageUrl.isNotEmpty() || it.bio.isNotEmpty() }
    }

    private fun readCache(file: File): ArtistInfo? {
        if (!file.exists()) return null
        return runCatching {
            val o = JSONObject(file.readText())
            val empty = o.optString("image").isEmpty() && o.optString("bio").isEmpty()
            val ttl = if (empty) 7L * DAY else 60L * DAY
            if (System.currentTimeMillis() - o.optLong("at") > ttl) null
            else ArtistInfo(o.optString("image"), o.optString("bio"), o.optString("url"))
        }.getOrNull()
    }

    private fun writeCache(file: File, info: ArtistInfo) {
        runCatching {
            file.writeText(
                JSONObject().put("image", info.imageUrl).put("bio", info.bio).put("url", info.bioUrl)
                    .put("at", System.currentTimeMillis()).toString()
            )
        }
    }

    /** Returns "" when Deezer answered but has no photo, null when it could not be reached. */
    private fun deezerImage(name: String): String? {
        val body = httpGet("https://api.deezer.com/search/artist?limit=10&q=" + enc(name))
            ?: return null
        val data = JSONObject(body).optJSONArray("data") ?: return ""
        val wanted = fold(name)
        val primary = fold(primaryArtist(name))
        var exact: JSONObject? = null
        var first: JSONObject? = null
        for (i in 0 until data.length()) {
            val o = data.getJSONObject(i)
            if (first == null) first = o
            val n = fold(o.optString("name"))
            if (n == wanted || n == primary) {
                exact = o
                break
            }
        }
        val pick = exact ?: first ?: return ""
        val url = pick.optString("picture_xl").ifEmpty { pick.optString("picture_big") }
        // Deezer's placeholder for artists without a photo has an empty image hash.
        return if (url.isEmpty() || url.contains("/artist//")) "" else url
    }

    /** Biography text and its page on Last.fm, in French when available, otherwise in English. */
    private fun lastFmBio(name: String): Pair<String, String>? {
        val key = BuildConfig.LASTFM_API_KEY
        if (key.isEmpty()) return null
        var reached = false
        for (artist in listOf(name, primaryArtist(name)).distinct()) {
            for (lang in listOf("fr", "")) {
                val url = "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&format=json&autocorrect=1&api_key=$key" +
                    "&artist=" + enc(artist) + (if (lang.isEmpty()) "" else "&lang=$lang")
                val body = httpGet(url) ?: continue
                reached = true
                val o = JSONObject(body).optJSONObject("artist") ?: continue
                val content = cleanBio(o.optJSONObject("bio")?.optString("content").orEmpty())
                if (content.isNotEmpty()) return content to o.optString("url")
            }
        }
        return if (reached) "" to "" else null
    }

    /** Removes the HTML link and the licence line Last.fm appends to every biography. */
    private fun cleanBio(raw: String): String {
        var t = raw
        val cut = t.indexOf("<a href=\"https://www.last.fm")
        if (cut >= 0) t = t.substring(0, cut)
        t = t.replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"").replace("&amp;", "&").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("\\n{3,}"), "\n\n")
        return t.trim()
    }

    private fun primaryArtist(name: String): String =
        name.split(Regex("(?i)\\s*(?:,|&|/|;|\\bfeat\\.?|\\bft\\.?|\\sx\\s)\\s*")).firstOrNull()?.trim().orEmpty().ifEmpty { name }

    private fun fold(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.lowercase().toByteArray()).joinToString("") { "%02x".format(it) }

    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Rostomusic/1.0 (Android)")
            if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
