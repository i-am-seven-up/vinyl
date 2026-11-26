package com.vinyl.shared.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Song::class, PlaybackSnapshot::class, PlaybackQueueItem::class, PlayEvent::class, Playlist::class, PlaylistItem::class], version = 3, exportSchema = true)
internal abstract class VinylDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun playbackDao(): PlaybackDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS playback_snapshot (id INTEGER NOT NULL, currentIndex INTEGER NOT NULL, positionMs INTEGER NOT NULL, repeatMode INTEGER NOT NULL, shuffleEnabled INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS playback_queue (position INTEGER NOT NULL, mediaId TEXT NOT NULL, uri TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, PRIMARY KEY(position))")
        db.execSQL("CREATE TABLE IF NOT EXISTS play_events (id TEXT NOT NULL, mediaId TEXT NOT NULL, startedAtMs INTEGER NOT NULL, listenedMs INTEGER NOT NULL, PRIMARY KEY(id))")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS playlist_songs (id TEXT NOT NULL, playlistId TEXT NOT NULL, songUri TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(songUri) REFERENCES songs(contentUri) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_songUri ON playlist_songs(songUri)")
    }
}
