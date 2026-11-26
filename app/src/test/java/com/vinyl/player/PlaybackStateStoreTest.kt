package com.vinyl.player

import com.vinyl.shared.database.PlaybackQueueItem
import com.vinyl.shared.database.PlaybackSnapshot
import com.vinyl.shared.database.SavedPlayback
import org.junit.Assert.*
import org.junit.Test

internal class PlaybackStateStoreTest {
    private val queue = listOf("a", "b", "a", "c").mapIndexed { index, id -> PlaybackQueueItem(index, id, id, id, "artist", "album") }
    private fun saved(index: Int) = SavedPlayback(PlaybackSnapshot(currentIndex = index, positionMs = 1234, repeatMode = 2, shuffleEnabled = true), queue)

    @Test fun removedEarlierItemKeepsCurrentDuplicateOccurrenceAndPosition() {
        val restored = filterPlayback(saved(2)) { it.position != 0 }
        assertEquals(listOf("b", "a", "c"), restored.queue.map { it.mediaId })
        assertEquals(1, restored.snapshot.currentIndex)
        assertEquals(1234L, restored.snapshot.positionMs)
        assertEquals(listOf(0, 1, 2), restored.queue.map { it.position })
        assertTrue(restored.snapshot.shuffleEnabled)
        assertEquals(2, restored.snapshot.repeatMode)
    }
    @Test fun missingCurrentSelectsNextSurvivorAtZero() {
        val restored = filterPlayback(saved(1)) { it.position != 1 }
        assertEquals(1, restored.snapshot.currentIndex)
        assertEquals(0L, restored.snapshot.positionMs)
    }
    @Test fun missingLastFallsBackToPreviousAndAllMissingIsEmpty() {
        assertEquals(2, filterPlayback(saved(3)) { it.position != 3 }.snapshot.currentIndex)
        val empty = filterPlayback(saved(3)) { false }
        assertTrue(empty.queue.isEmpty())
        assertEquals(0L, empty.snapshot.positionMs)
        assertEquals(0, empty.snapshot.currentIndex)
    }
}
