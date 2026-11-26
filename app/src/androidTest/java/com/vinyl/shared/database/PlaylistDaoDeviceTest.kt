package com.vinyl.shared.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class PlaylistDaoDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun song(uri: String) = Song(uri, "album", "artist", uri, "Artist", "Album", uri, "artist", "album", 31000, 1, 0, null, "external")

    @Test fun duplicateOccurrencesReorderRemoveAndRescanPersist() = runBlocking {
        val name = "b4-playlist-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, VinylDatabase::class.java, name).build()
        var db = open()
        try {
            val songs = db.songDao()
            val dao = db.playlistDao()
            songs.replaceSongs(listOf(song("one"), song("two")))
            dao.createPlaylist("p", "  Favorites  ")
            assertEquals("Favorites", dao.findPlaylist("p")?.name)
            try { dao.renamePlaylist("p", "  "); fail("Blank name accepted") } catch (_: IllegalArgumentException) { }
            dao.addSong("p", "a", "one")
            dao.addSong("p", "b", "two")
            dao.addSong("p", "c", "one")
            dao.moveItem("p", "c", -1)
            assertEquals(listOf("a", "c", "b"), dao.getItems("p").map { it.id })
            dao.removeItem("p", "a")
            assertEquals(listOf("c", "b"), dao.getItems("p").map { it.id })
            assertEquals(listOf(0, 1), dao.getItems("p").map { it.position })
            songs.replaceSongs(listOf(song("one").copy(title = "Updated"), song("two")))
            assertEquals(2, dao.getItems("p").size)
            assertEquals("Updated", dao.observeSongs("p").first().first().song.title)
            dao.renamePlaylist("p", "Renamed")
            db.close()
            db = open()
            assertEquals("Renamed", db.playlistDao().findPlaylist("p")?.name)
            assertEquals(listOf("c", "b"), db.playlistDao().getItems("p").map { it.id })
            db.songDao().replaceSongs(listOf(song("two")))
            assertEquals(listOf("b"), db.playlistDao().getItems("p").map { it.id })
            try { db.playlistDao().addSong("p", "missing", "one"); fail("Missing song accepted") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            db.playlistDao().deletePlaylist("p")
            assertTrue(db.playlistDao().getItems("p").isEmpty())
            assertEquals(1, db.songDao().getSongs().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationFromV2PreservesPlaybackAndSongs() = runBlocking {
        val name = "b4-migration-${System.nanoTime()}.db"
        val legacy = context.openOrCreateDatabase(name, 0, null)
        try {
            legacy.execSQL("CREATE TABLE songs (contentUri TEXT NOT NULL PRIMARY KEY, albumId TEXT NOT NULL, artistId TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, titleNorm TEXT NOT NULL, artistNorm TEXT NOT NULL, albumNorm TEXT NOT NULL, durationMs INTEGER NOT NULL, trackNumber INTEGER NOT NULL, dateModified INTEGER NOT NULL, relativePath TEXT, volumeName TEXT NOT NULL)")
            legacy.execSQL("INSERT INTO songs VALUES ('one','album','artist','Title','Artist','Album','title','artist','album',31000,1,0,NULL,'external')")
            legacy.execSQL("CREATE TABLE playback_snapshot (id INTEGER NOT NULL PRIMARY KEY, currentIndex INTEGER NOT NULL, positionMs INTEGER NOT NULL, repeatMode INTEGER NOT NULL, shuffleEnabled INTEGER NOT NULL)")
            legacy.execSQL("CREATE TABLE playback_queue (position INTEGER NOT NULL PRIMARY KEY, mediaId TEXT NOT NULL, uri TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL)")
            legacy.execSQL("CREATE TABLE play_events (id TEXT NOT NULL PRIMARY KEY, mediaId TEXT NOT NULL, startedAtMs INTEGER NOT NULL, listenedMs INTEGER NOT NULL)")
            legacy.execSQL("INSERT INTO playback_snapshot VALUES (0,0,1700,2,1)")
            legacy.execSQL("INSERT INTO playback_queue VALUES (0,'one','one','Title','Artist','Album')")
            legacy.execSQL("INSERT INTO play_events VALUES ('event','one',1000,500)")
            legacy.version = 2
        } finally { legacy.close() }
        val db = Room.databaseBuilder(context, VinylDatabase::class.java, name).addMigrations(MIGRATION_2_3).build()
        try {
            assertEquals("Title", db.songDao().getSongs().single().title)
            assertEquals(1700L, db.playbackDao().findPlayback()?.snapshot?.positionMs)
            assertEquals(1, db.playbackDao().findPlayback()?.queue?.size)
            assertEquals(500L, db.playbackDao().getPlayEvents().single().listenedMs)
            db.playlistDao().createPlaylist("p", "Playlist")
            db.playlistDao().addSong("p", "item", "one")
            assertEquals(1, db.playlistDao().getItems("p").size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
