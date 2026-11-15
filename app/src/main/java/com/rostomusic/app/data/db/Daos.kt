package com.rostomusic.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<Song>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<Song>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: Long): Song?

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(songs: List<Song>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE songs SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + :counted, lastPlayed = :at, listenedMs = listenedMs + :listened WHERE id = :id")
    suspend fun recordPlay(id: Long, at: Long, listened: Long, counted: Int)

    @Query("UPDATE songs SET favorite = :favorite, playCount = :playCount, lastPlayed = :lastPlayed, listenedMs = :listenedMs WHERE path = :path")
    suspend fun restoreStats(path: String, favorite: Boolean, playCount: Int, lastPlayed: Long, listenedMs: Long)

    /** Replaces the scanned library while keeping favourites and listening stats. */
    @Transaction
    suspend fun replaceScanned(scanned: List<Song>) {
        val existing = getAll().associateBy { it.id }
        val merged = scanned.map { s ->
            val old = existing[s.id]
            if (old == null) s
            else s.copy(favorite = old.favorite, playCount = old.playCount, lastPlayed = old.lastPlayed, listenedMs = old.listenedMs)
        }
        val keep = scanned.mapTo(HashSet()) { it.id }
        val gone = existing.keys.filter { it !in keep }
        gone.chunked(500).forEach { deleteByIds(it) }
        merged.chunked(500).forEach { upsert(it) }
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position")
    fun observeEntries(): Flow<List<PlaylistSong>>

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylists(): List<Playlist>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    suspend fun entries(playlistId: Long): List<PlaylistSong>

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    suspend fun clear(id: Long)

    @Insert
    suspend fun insertEntries(entries: List<PlaylistSong>)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_songs WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Transaction
    suspend fun append(playlistId: Long, songIds: List<Long>) {
        var pos = maxPosition(playlistId) + 1
        insertEntries(songIds.map { PlaylistSong(playlistId = playlistId, songId = it, position = pos++) })
    }

    @Transaction
    suspend fun replace(playlistId: Long, songIds: List<Long>) {
        clear(playlistId)
        insertEntries(songIds.mapIndexed { i, id -> PlaylistSong(playlistId = playlistId, songId = id, position = i) })
    }

    @Transaction
    suspend fun delete(playlistId: Long) {
        clear(playlistId)
        deletePlaylist(playlistId)
    }
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry)

    @Query("SELECT * FROM history WHERE playedAt >= :since ORDER BY playedAt DESC")
    fun observeSince(since: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntry>>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM history")
    fun observeTotalListened(): Flow<Long>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun get(songId: Long): LyricsEntity?

    @Query("SELECT * FROM lyrics")
    suspend fun getAll(): List<LyricsEntity>

    @Query("SELECT songId FROM lyrics WHERE content LIKE '%' || :query || '%' LIMIT 50")
    suspend fun search(query: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun delete(songId: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY positionMs")
    fun observe(songId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<Bookmark>

    @Insert
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)
}

@Dao
interface WaveformDao {
    @Query("SELECT * FROM waveforms WHERE songId = :songId")
    suspend fun get(songId: Long): WaveformEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: WaveformEntity)
}

@Dao
interface SongQualityDao {
    @Query("SELECT * FROM song_quality")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<SongQuality>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SongQuality)
}
