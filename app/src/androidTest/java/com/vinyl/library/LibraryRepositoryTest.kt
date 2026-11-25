package com.vinyl.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vinyl.shared.database.Song
import com.vinyl.shared.database.VinylDatabase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LibraryRepositoryTest {
    private lateinit var database: VinylDatabase

    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VinylDatabase::class.java).build()
    }

    @After fun closeDatabase() { database.close() }

    @Test fun rescanReplacesMetadataRemovesStaleSongsAndDoesNotDuplicate() = runBlocking {
        val dao = database.songDao()
        var source = listOf(song(1), song(2))
        val repository = LibraryRepository({ source }, dao)
        repository.scanLibrary()
        repository.scanLibrary()
        assertEquals(2, dao.getSongs().size)
        source = listOf(song(2).copy(title = "Updated"))
        repository.scanLibrary()
        assertEquals(source, dao.getSongs())
        source = emptyList()
        repository.scanLibrary()
        assertEquals(emptyList<Song>(), dao.getSongs())
    }

    @Test fun failedDeniedAndCancelledScansPreserveSavedLibrary() = runBlocking {
        val dao = database.songDao()
        val saved = listOf(song(1))
        dao.replaceSongs(saved)
        for (failure in listOf(IOException("Query failed"), SecurityException("Denied"), CancellationException("Cancelled"))) {
            val repository = LibraryRepository({ throw failure }, dao)
            assertThrows(failure.javaClass) { runBlocking { repository.scanLibrary() } }
            assertEquals(saved, dao.getSongs())
        }
    }

    @Test fun invalidSnapshotRollsBackDeletion() = runBlocking {
        val dao = database.songDao()
        val saved = listOf(song(1))
        dao.replaceSongs(saved)
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            runBlocking { dao.replaceSongs(listOf(song(2), song(2))) }
        }
        assertEquals(saved, dao.getSongs())
    }

    @Test fun observationDoesNotScanAndAlbumsGroupByIdWithTracksInOrder() = runBlocking {
        val dao = database.songDao()
        dao.replaceSongs(listOf(song(2), song(1), song(3).copy(albumId = "external:99")))
        val repository = LibraryRepository({ error("Observe must never scan") }, dao)
        assertEquals(listOf(2, 1), repository.observeAlbums().first().map { it.songCount })
        assertEquals(listOf(1, 2), repository.observeAlbumSongs("external:1").first().map { it.trackNumber })
    }

    @Test fun snapshotSupportsThreeThousandSongsWithoutSqlParameterLimit() = runBlocking {
        val repository = LibraryRepository({ (1..3000).map { song(it) } }, database.songDao())
        repository.scanLibrary()
        repository.scanLibrary()
        assertEquals(3000, database.songDao().getSongs().size)
    }

    private fun song(id: Int) = Song(
        contentUri = "content://media/external/audio/media/$id", albumId = "external:1", artistId = "external:1",
        title = "Song $id", artist = "Artist", album = "Album", titleNorm = "song $id",
        artistNorm = "artist", albumNorm = "album", durationMs = 60_000, trackNumber = id,
        dateModified = 1, relativePath = "Music/Test/", volumeName = "external",
    )
}
