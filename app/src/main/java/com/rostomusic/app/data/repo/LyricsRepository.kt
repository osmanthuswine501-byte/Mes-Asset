package com.rostomusic.app.data.repo

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.rostomusic.app.data.db.LyricsEntity
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.data.tags.TagEditor
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.domain.Lyrics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

enum class LyricsSource { USER, ONLINE, TAG_SYNCED, LRC_FILE, TAG_PLAIN, NONE }

data class LoadedLyrics(
    val songId: Long,
    val lyrics: Lyrics,
    val source: LyricsSource,
    val userOffsetMs: Long,
    val raw: String,
) {
    companion object {
        fun empty(songId: Long, offset: Long = 0) = LoadedLyrics(songId, Lyrics(emptyList(), false), LyricsSource.NONE, offset, "")
    }
}

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RostoDatabase,
    private val tags: TagEditor,
    private val settings: SettingsStore,
) {
    private var lrcIndex: Map<String, Uri>? = null
    private var lrcIndexTree: String = ""

    /**
     * Lyrics saved in the app win, then synced tag, then the .lrc file next to the song, then the plain tag.
     */
    suspend fun load(song: Song): LoadedLyrics = withContext(Dispatchers.IO) {
        val saved = db.lyrics().get(song.id)
        val offset = saved?.offsetMs ?: 0
        if (saved != null && saved.content.isNotBlank()) {
            val src = if (saved.source == LyricsSource.ONLINE.name) LyricsSource.ONLINE else LyricsSource.USER
            return@withContext LoadedLyrics(song.id, LrcParser.parse(saved.content), src, offset, saved.content)
        }
        val embedded = runCatching { tags.readEmbeddedLyrics(song) }.getOrNull()
        embedded?.synced?.let { return@withContext LoadedLyrics(song.id, LrcParser.parse(it), LyricsSource.TAG_SYNCED, offset, it) }
        readSidecar(song)?.let { return@withContext LoadedLyrics(song.id, LrcParser.parse(it), LyricsSource.LRC_FILE, offset, it) }
        embedded?.plain?.let { return@withContext LoadedLyrics(song.id, LrcParser.parse(it), LyricsSource.TAG_PLAIN, offset, it) }
        LoadedLyrics.empty(song.id, offset)
    }

    suspend fun save(songId: Long, raw: String, source: LyricsSource) {
        val old = db.lyrics().get(songId)
        db.lyrics().put(LyricsEntity(songId, raw.trim(), source.name, old?.offsetMs ?: 0))
    }

    suspend fun setOffset(songId: Long, offsetMs: Long) {
        val old = db.lyrics().get(songId)
        db.lyrics().put(LyricsEntity(songId, old?.content.orEmpty(), old?.source ?: LyricsSource.NONE.name, offsetMs))
    }

    /** Removes the lyrics saved in the app; tag or file lyrics show again. */
    suspend fun deleteSaved(songId: Long) = db.lyrics().delete(songId)

    private suspend fun readSidecar(song: Song): String? {
        val base = song.path.substringBeforeLast('.')
        for (ext in listOf(".lrc", ".LRC")) {
            val f = File(base + ext)
            if (f.canRead()) runCatching { return decode(f.readBytes()) }
        }
        val tree = settings.current().lrcTreeUri
        if (tree.isEmpty()) return null
        val index = indexFor(tree)
        val name = song.path.substringAfterLast('/').substringBeforeLast('.').lowercase()
        val uri = index[name] ?: return null
        return runCatching { context.contentResolver.openInputStream(uri)?.use { decode(it.readBytes()) } }.getOrNull()
    }

    /** Lists the .lrc files of the folder the user allowed, once, so lookups stay instant. */
    private fun indexFor(tree: String): Map<String, Uri> {
        lrcIndex?.let { if (lrcIndexTree == tree) return it }
        val map = HashMap<String, Uri>()
        runCatching {
            val treeUri = Uri.parse(tree)
            val pending = ArrayDeque<String>()
            pending += DocumentsContract.getTreeDocumentId(treeUri)
            var visited = 0
            while (pending.isNotEmpty() && visited < 3000) {
                val docId = pending.removeFirst()
                visited++
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                context.contentResolver.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val name = c.getString(1).orEmpty()
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) pending += id
                        else if (name.endsWith(".lrc", ignoreCase = true)) {
                            map[name.substringBeforeLast('.').lowercase()] = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        }
                    }
                }
            }
        }
        lrcIndex = map
        lrcIndexTree = tree
        return map
    }

    fun invalidateLrcIndex() {
        lrcIndex = null
    }

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if ((b0 == 0xFF && b1 == 0xFE) || (b0 == 0xFE && b1 == 0xFF)) return String(bytes, Charsets.UTF_16)
        }
        val utf8 = String(bytes, Charsets.UTF_8)
        return if (utf8.contains('�')) String(bytes, charset("windows-1252")) else utf8
    }

    /** Optional online lookup on LRCLIB. Only called when the user turned the option on. */
    suspend fun fetchOnline(song: Song): String? = withContext(Dispatchers.IO) {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val exact = "https://lrclib.net/api/get?track_name=${enc(song.title)}&artist_name=${enc(song.artist)}" +
            "&album_name=${enc(song.album)}&duration=${song.duration / 1000}"
        httpGet(exact)?.let { body ->
            runCatching { pick(JSONObject(body)) }.getOrNull()?.let { return@withContext it }
        }
        val search = "https://lrclib.net/api/search?track_name=${enc(song.title)}&artist_name=${enc(song.artist)}"
        httpGet(search)?.let { body ->
            runCatching {
                val arr = JSONArray(body)
                var plain: String? = null
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                    if (synced != null) return@withContext synced
                    if (plain == null) plain = o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
                }
                plain
            }.getOrNull()?.let { return@withContext it }
        }
        null
    }

    private fun pick(o: JSONObject): String? =
        o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
            ?: o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }

    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Rostomusic/1.0 (Android)")
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
