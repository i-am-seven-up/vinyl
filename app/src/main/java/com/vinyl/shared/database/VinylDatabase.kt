package com.vinyl.shared.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Song::class, PlaybackSnapshot::class, PlaybackQueueItem::class, PlayEvent::class], version = 2, exportSchema = true)
internal abstract class VinylDatabase : RoomDatabase() {
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
