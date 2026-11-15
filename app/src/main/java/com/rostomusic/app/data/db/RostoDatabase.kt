package com.rostomusic.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Song::class, Playlist::class, PlaylistSong::class, HistoryEntry::class, LyricsEntity::class, Bookmark::class, WaveformEntity::class, SongQuality::class],
    version = 4,
    exportSchema = true,
)
abstract class RostoDatabase : RoomDatabase() {
    abstract fun songs(): SongDao
    abstract fun playlists(): PlaylistDao
    abstract fun history(): HistoryDao
    abstract fun lyrics(): LyricsDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun waveforms(): WaveformDao
    abstract fun quality(): SongQualityDao
}

/** Version 2 adds the column for files read straight from storage. Nothing else changes, so no data is lost. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `contentUri` TEXT NOT NULL DEFAULT ''")
    }
}

/** Version 3 adds a separate table for codec and bit depth. Existing data is untouched. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `song_quality` (`songId` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL, `codecMime` TEXT NOT NULL, `bitDepth` INTEGER NOT NULL, `explicit` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`songId`))")
    }
}

/** Version 4 adds the explicit flag; the quality table is rebuilt and read again in the background. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `song_quality`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `song_quality` (`songId` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL, `codecMime` TEXT NOT NULL, `bitDepth` INTEGER NOT NULL, `explicit` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`songId`))")
    }
}
