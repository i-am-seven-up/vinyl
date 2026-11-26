package com.vinyl.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vinyl.library.LibraryRepository
import com.vinyl.library.normalizeSearchText
import com.vinyl.shared.database.Song
import com.vinyl.shared.database.VinylDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class SearchDeviceTest {
    private fun song(id: String, title: String, artist: String = "Nghệ sĩ", album: String = "Album") = Song(
        id, "album", "artist", title, artist, album, normalizeSearchText(title), normalizeSearchText(artist), normalizeSearchText(album), 31000, 1, 0, null, "external")

    @Test fun normalizedSearchBindsAndEscapesWildcards() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VinylDatabase::class.java).build()
        try {
            db.songDao().replaceSongs(listOf(song("1", "Đường về", "Đặng", "Mùa hè"), song("2", "100%_! mix"), song("3", "100abc mix"), song("4", "O'Reilly")))
            val repo = LibraryRepository({ error("Search must not scan") }, db.songDao())
            assertEquals(listOf("1"), repo.observeSearchSongs("  DUONG VE  ").first().map { it.contentUri })
            assertEquals(listOf("1"), repo.observeSearchSongs("dang").first().map { it.contentUri })
            assertEquals(listOf("1"), repo.observeSearchSongs("mua he").first().map { it.contentUri })
            for (query in listOf("%", "_", "!", "%_!")) assertEquals(listOf("2"), repo.observeSearchSongs(query).first().map { it.contentUri })
            assertEquals(listOf("4"), repo.observeSearchSongs("O'Reilly").first().map { it.contentUri })
            assertTrue(repo.observeSearchSongs("' OR 1=1 --").first().isEmpty())
        } finally { db.close() }
    }

    @Test fun debounceUsesLatestQueryAndBlankClearsResults() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VinylDatabase::class.java).build()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val models = ViewModelStore()
        lateinit var model: SearchViewModel
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + 5_000_000_000
            while (!condition() && System.nanoTime() < deadline) Thread.sleep(20)
            assertTrue("Search did not settle", condition())
        }
        try {
            db.songDao().replaceSongs(listOf(song("1", "Đường về"), song("2", "Khác")))
            instrumentation.runOnMainSync {
                model = SearchViewModel(LibraryRepository({ emptyList() }, db.songDao()), SavedStateHandle())
                models.put("search", model)
                model.setQuery("Khác")
                model.setQuery("duong")
            }
            await { !model.uiState.value.isLoading && model.uiState.value.songs.size == 1 }
            assertEquals("1", model.uiState.value.songs.single().contentUri)
            instrumentation.runOnMainSync { model.setQuery("duong") }
            assertFalse(model.uiState.value.isLoading)
            instrumentation.runOnMainSync { model.setQuery(" ") }
            await { !model.uiState.value.isLoading && model.uiState.value.songs.isEmpty() }
            db.songDao().replaceSongs(emptyList())
            instrumentation.runOnMainSync { model.setQuery("duong") }
            await { !model.uiState.value.isLoading }
            assertTrue(model.uiState.value.songs.isEmpty())
        } finally { instrumentation.runOnMainSync { models.clear() }; db.close() }
    }
}
