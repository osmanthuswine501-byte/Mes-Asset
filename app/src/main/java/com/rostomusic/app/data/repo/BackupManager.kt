package com.rostomusic.app.data.repo

import android.content.Context
import android.net.Uri
import com.rostomusic.app.data.db.Bookmark
import com.rostomusic.app.data.db.LyricsEntity
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.domain.M3u
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Backup, restore and playlist files. Songs are matched by file path because system ids change between phones. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RostoDatabase,
    private val library: LibraryRepository,
    private val settings: com.rostomusic.app.data.settings.SettingsStore,
) {
    suspend fun export(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val songs = db.songs().getAll()
            val byId = songs.associateBy { it.id }
            val root = JSONObject()
            root.put("app", "Rostomusic")
            root.put("version", 1)
            root.put("createdAt", System.currentTimeMillis())
            root.put("homeLayout", settings.current().homeLayout)
            root.put("songs", JSONArray().apply {
                songs.filter { it.favorite || it.playCount > 0 || it.listenedMs > 0 }.forEach { s ->
                    put(JSONObject().put("path", s.path).put("favorite", s.favorite).put("playCount", s.playCount)
                        .put("lastPlayed", s.lastPlayed).put("listenedMs", s.listenedMs))
                }
            })
            root.put("playlists", JSONArray().apply {
                db.playlists().getPlaylists().forEach { p ->
                    val paths = JSONArray()
                    db.playlists().entries(p.id).forEach { e -> byId[e.songId]?.let { paths.put(it.path) } }
                    put(JSONObject().put("name", p.name).put("createdAt", p.createdAt).put("songs", paths))
                }
            })
            root.put("lyrics", JSONArray().apply {
                db.lyrics().getAll().forEach { l ->
                    byId[l.songId]?.let { s ->
                        put(JSONObject().put("path", s.path).put("content", l.content).put("source", l.source).put("offsetMs", l.offsetMs))
                    }
                }
            })
            root.put("bookmarks", JSONArray().apply {
                db.bookmarks().getAll().forEach { b ->
                    byId[b.songId]?.let { s ->
                        put(JSONObject().put("path", s.path).put("positionMs", b.positionMs).put("label", b.label).put("createdAt", b.createdAt))
                    }
                }
            })
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString(2).toByteArray()) } ?: error("unwritable")
        }
    }

    /** Returns the number of restored items. */
    suspend fun restore(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("unreadable")
            val root = JSONObject(text)
            require(root.optString("app") == "Rostomusic") { "format" }
            val byPath = db.songs().getAll().associateBy { it.path }
            var restored = 0
            root.optString("homeLayout").takeIf { it.isNotBlank() }?.let { settings.setHomeLayout(it); restored++ }
            root.optJSONArray("songs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val path = o.getString("path")
                    if (byPath.containsKey(path)) {
                        db.songs().restoreStats(path, o.optBoolean("favorite"), o.optInt("playCount"), o.optLong("lastPlayed"), o.optLong("listenedMs"))
                        restored++
                    }
                }
            }
            root.optJSONArray("playlists")?.let { arr ->
                val existing = db.playlists().getPlaylists().map { it.name }.toSet()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val paths = o.optJSONArray("songs") ?: JSONArray()
                    val ids = (0 until paths.length()).mapNotNull { byPath[paths.getString(it)]?.id }
                    var name = o.getString("name")
                    if (name in existing) name = "$name (2)"
                    library.createPlaylist(name, ids)
                    restored++
                }
            }
            root.optJSONArray("lyrics")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    byPath[o.getString("path")]?.let { s ->
                        db.lyrics().put(LyricsEntity(s.id, o.optString("content"), o.optString("source"), o.optLong("offsetMs")))
                        restored++
                    }
                }
            }
            root.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    byPath[o.getString("path")]?.let { s ->
                        db.bookmarks().insert(Bookmark(songId = s.id, positionMs = o.optLong("positionMs"), label = o.optString("label"), createdAt = o.optLong("createdAt")))
                        restored++
                    }
                }
            }
            restored
        }
    }

    suspend fun exportPlaylist(uri: Uri, songs: List<Song>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = M3u.build(songs.map { M3u.Entry(it.path, "${it.artist} - ${it.title}", it.duration / 1000) })
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("unwritable")
        }
    }

    /** Imports an M3U or M3U8 file as a new playlist. Returns matched and total entry counts. */
    suspend fun importPlaylist(uri: Uri, name: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("unreadable")
            val entries = M3u.parse(text)
            val all = db.songs().getAll()
            val byPath = all.associateBy { it.path }
            val byName = all.groupBy { M3u.fileName(it.path).lowercase() }
            val ids = entries.mapNotNull { e ->
                byPath[e.path]?.id ?: byName[M3u.fileName(e.path).lowercase()]?.firstOrNull()?.id
            }
            if (ids.isNotEmpty()) library.createPlaylist(name, ids)
            ids.size to entries.size
        }
    }
}
