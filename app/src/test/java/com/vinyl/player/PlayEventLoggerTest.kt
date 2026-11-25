package com.vinyl.player

import org.junit.Assert.*
import org.junit.Test

internal class PlayEventLoggerTest {
    @Test fun pauseBufferingAndSeekDoNotAddPositionOrWallClockTime() {
        var elapsed = 0L
        var wall = 1000L
        val logger = PlayEventLogger({ elapsed }, { wall })
        logger.updatePlayback("song", false)
        elapsed = 500
        logger.updatePlayback("song", true)
        elapsed = 1500
        logger.checkpoint()
        assertEquals(1000L, logger.getEvents().single().listenedMs)
        logger.updatePlayback("song", false) // paused or buffering
        elapsed = 10_000
        wall = -100_000 // manual clock change must not affect elapsed listening
        logger.updatePlayback("song", false)
        assertEquals(1000L, logger.getEvents().single().listenedMs)
        logger.updatePlayback("song", true)
        elapsed = 10_200
        logger.updatePlayback("song", true) // seek: position is deliberately not an input
        elapsed = 10_500
        logger.updatePlayback("song", false)
        assertEquals(1500L, logger.getEvents().single().listenedMs)
        assertEquals(1000L, logger.getEvents().single().startedAtMs)
    }

    @Test fun repeatedMediaIdStartsNewEventAndCheckpointDoesNotDoubleCount() {
        var time = 0L
        val logger = PlayEventLogger({ time }, { time })
        logger.updatePlayback("same", true)
        time = 1000
        logger.checkpoint()
        logger.checkpoint()
        logger.updatePlayback("same", true, isNewItem = true)
        time = 1500
        logger.checkpoint()
        assertEquals(listOf(1000L, 500L), logger.getEvents().map { it.listenedMs })
        assertEquals(2, logger.getEvents().map { it.id }.distinct().size)
    }

    @Test fun restorePausedDoesNotCreateHistory() {
        val logger = PlayEventLogger({ 5000 }, { 1000 })
        logger.updatePlayback("song", false, true)
        logger.checkpoint()
        assertTrue(logger.getEvents().isEmpty())
    }
}
