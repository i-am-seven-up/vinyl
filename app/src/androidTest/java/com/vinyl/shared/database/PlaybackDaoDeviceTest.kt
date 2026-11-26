package com.vinyl.shared.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class PlaybackDaoDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun migrationPreservesV1SongsAndPlaybackSnapshotIsAtomic() = runBlocking {
        val name = "b3-migration-${System.nanoTime()}.db"
        val legacy = context.openOrCreateDatabase(name, 0, null)
        try {
            legacy.execSQL("CREATE TABLE songs (contentUri TEXT NOT NULL PRIMARY KEY, albumId TEXT NOT NULL, artistId TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, titleNorm TEXT NOT NULL, artistNorm TEXT NOT NULL, albumNorm TEXT NOT NULL, durationMs INTEGER NOT NULL, trackNumber INTEGER NOT NULL, dateModified INTEGER NOT NULL, relativePath TEXT, volumeName TEXT NOT NULL)")
            legacy.execSQL("INSERT INTO songs VALUES ('content://song/1','album','artist','Title','Artist','Album','title','artist','album',31000,1,0,NULL,'external')")
            legacy.version = 1
        } finally { legacy.close() }
        val database = Room.databaseBuilder(context, VinylDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        try {
            assertEquals("Title", database.songDao().getSongs().single().title)
            val dao = database.playbackDao()
            val snapshot = PlaybackSnapshot(currentIndex = 1, positionMs = 1200, repeatMode = 2, shuffleEnabled = true)
            val queue = listOf(0, 1).map { PlaybackQueueItem(it, "same", "content://same", "Title", "Artist", "Album") }
            val event = PlayEvent("event", "same", 1000, 200)
            dao.savePlayback(snapshot, queue, listOf(event))
            dao.savePlayback(snapshot, queue, listOf(event.copy(listenedMs = 500)))
            assertEquals(SavedPlayback(snapshot, queue), dao.findPlayback())
            assertEquals(500L, dao.getPlayEvents().single().listenedMs)
            try {
                dao.savePlayback(snapshot.copy(positionMs = 9999), listOf(queue[0], queue[0]), emptyList())
                fail("Duplicate queue position must roll back")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(SavedPlayback(snapshot, queue), dao.findPlayback())
            database.songDao().replaceSongs(emptyList())
            assertEquals(1, dao.getPlayEvents().size)
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
