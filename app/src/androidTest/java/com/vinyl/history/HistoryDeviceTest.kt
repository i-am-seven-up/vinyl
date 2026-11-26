package com.vinyl.history

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vinyl.shared.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class HistoryDeviceTest {
    private val snapshot = PlaybackSnapshot(currentIndex = 0, positionMs = 0, repeatMode = 0, shuffleEnabled = false)
    private fun song() = Song("song", "album", "artist", "Đường về", "Vinyl", "Album", "duong ve", "vinyl", "album", 31000, 1, 0, null, "external")

    @Test fun historyKeepsOccurrencesSortsNewestAndSurvivesSongDeletion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VinylDatabase::class.java).build()
        try {
            val dao = db.playbackDao()
            assertTrue(dao.observeHistory(100).first().isEmpty())
            db.songDao().replaceSongs(listOf(song()))
            val events = listOf(PlayEvent("a", "song", 1000, 200), PlayEvent("b", "song", 2000, 1000),
                PlayEvent("c", "deleted", 2000, 500), PlayEvent("zero", "song", 3000, 0))
            dao.savePlayback(snapshot, emptyList(), events)
            val entries = dao.observeHistory(100).first()
            assertEquals(listOf("c", "b", "a"), entries.map { it.event.id })
            assertNull(entries.first().title)
            assertEquals("Đường về", entries[1].title)
            assertEquals(listOf("c", "b"), dao.observeHistory(2).first().map { it.event.id })
            dao.savePlayback(snapshot, emptyList(), listOf(events[1].copy(listenedMs = 2000)))
            assertEquals(3, dao.observeHistory(100).first().size)
            assertEquals(2000L, dao.observeHistory(100).first()[1].event.listenedMs)
            db.songDao().replaceSongs(emptyList())
            assertTrue(dao.observeHistory(100).first().all { it.title == null && it.artist == null })
            assertEquals(4, dao.getPlayEvents().size)
        } finally { db.close() }
    }

    @Test fun viewModelLoadsMoreAndObservesCheckpointUpdatesWithoutWriting() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VinylDatabase::class.java).build()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val models = ViewModelStore()
        lateinit var model: HistoryViewModel
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + 5_000_000_000
            while (!condition() && System.nanoTime() < deadline) Thread.sleep(20)
            assertTrue("History did not settle", condition())
        }
        try {
            val events = (1..105).map { PlayEvent("event-$it", "song", it.toLong(), 1000) }
            db.playbackDao().savePlayback(snapshot, emptyList(), events)
            instrumentation.runOnMainSync { model = HistoryViewModel(db.playbackDao()); models.put("history", model) }
            await { !model.uiState.value.isLoading }
            assertEquals(100, model.uiState.value.items.size)
            assertTrue(model.uiState.value.hasMore)
            instrumentation.runOnMainSync { model.loadMore() }
            await { !model.uiState.value.isLoading && model.uiState.value.items.size == 105 }
            assertFalse(model.uiState.value.hasMore)
            db.playbackDao().savePlayback(snapshot, emptyList(), listOf(events.last().copy(listenedMs = 9000)))
            await { model.uiState.value.items.first().event.listenedMs == 9000L }
            instrumentation.runOnMainSync { model.retry() }
            await { !model.uiState.value.isLoading }
            assertEquals(105, db.playbackDao().getPlayEvents().size)
            assertEquals(snapshot, db.playbackDao().findSnapshot())
        } finally { instrumentation.runOnMainSync { models.clear() }; db.close() }
    }
}
