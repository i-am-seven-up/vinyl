package com.vinyl.shared.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_snapshot")
internal data class PlaybackSnapshot(
    @PrimaryKey val id: Int = 0,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
)

@Entity(tableName = "playback_queue")
internal data class PlaybackQueueItem(
    @PrimaryKey val position: Int,
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
)

// No foreign key to songs: rescans/deleted files must not erase listening history.
@Entity(tableName = "play_events")
internal data class PlayEvent(
    @PrimaryKey val id: String,
    val mediaId: String,
    val startedAtMs: Long,
    val listenedMs: Long,
)

internal data class HistoryEntry(
    @Embedded val event: PlayEvent,
    val title: String?,
    val artist: String?,
)

internal data class SavedPlayback(val snapshot: PlaybackSnapshot, val queue: List<PlaybackQueueItem>)

@Dao
internal abstract class PlaybackDao {
    @Query("SELECT * FROM playback_snapshot WHERE id = 0")
    abstract suspend fun findSnapshot(): PlaybackSnapshot?
    @Query("SELECT * FROM playback_queue ORDER BY position")
    abstract suspend fun getQueue(): List<PlaybackQueueItem>
    @Query("SELECT * FROM play_events ORDER BY startedAtMs, id")
    abstract suspend fun getPlayEvents(): List<PlayEvent>

    @Query("""SELECT e.*, s.title, s.artist FROM play_events e
        LEFT JOIN songs s ON s.contentUri = e.mediaId
        WHERE e.listenedMs > 0 ORDER BY e.startedAtMs DESC, e.id DESC LIMIT :limit""")
    abstract fun observeHistory(limit: Int): Flow<List<HistoryEntry>>

    @Transaction
    open suspend fun findPlayback(): SavedPlayback? {
        val snapshot = findSnapshot() ?: return null
        return SavedPlayback(snapshot, getQueue())
    }

    @Transaction
    open suspend fun savePlayback(snapshot: PlaybackSnapshot, queue: List<PlaybackQueueItem>, events: List<PlayEvent>) {
        saveSnapshot(snapshot)
        deleteQueue()
        insertQueue(queue)
        saveEvents(events)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun saveSnapshot(snapshot: PlaybackSnapshot)
    @Query("DELETE FROM playback_queue")
    protected abstract suspend fun deleteQueue()
    @Insert
    protected abstract suspend fun insertQueue(queue: List<PlaybackQueueItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun saveEvents(events: List<PlayEvent>)
}
