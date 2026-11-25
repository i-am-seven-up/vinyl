package com.vinyl.player

import com.vinyl.shared.database.PlayEvent
import java.util.UUID

// Listening time uses only the monotonic clock, never seek position or wall-clock differences.
internal class PlayEventLogger(
    private val elapsedRealtimeMs: () -> Long,
    private val currentTimeMs: () -> Long,
) {
    private var current: PlayEvent? = null
    private var finished: PlayEvent? = null
    private var lastElapsedMs = elapsedRealtimeMs()
    private var isPlaying = false

    fun updatePlayback(mediaId: String?, playing: Boolean, isNewItem: Boolean = false) {
        checkpoint()
        if (current?.mediaId != mediaId || isNewItem) {
            if (current != null) finished = current
            current = null
        }
        // Do not create empty listening events just because a paused queue was restored.
        if (current == null && mediaId != null && playing) {
            current = PlayEvent(UUID.randomUUID().toString(), mediaId, currentTimeMs(), 0)
        }
        isPlaying = playing
    }

    fun checkpoint() {
        val now = elapsedRealtimeMs()
        if (isPlaying) current = current?.copy(listenedMs = (current?.listenedMs ?: 0) + (now - lastElapsedMs).coerceAtLeast(0))
        lastElapsedMs = now
    }

    fun getEvents(): List<PlayEvent> = listOfNotNull(finished, current).filter { it.listenedMs > 0 }
}
