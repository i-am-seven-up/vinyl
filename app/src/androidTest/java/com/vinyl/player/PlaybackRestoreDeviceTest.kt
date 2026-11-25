package com.vinyl.player

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vinyl.VinylApplication
import com.vinyl.shared.database.PlaybackQueueItem
import com.vinyl.shared.database.PlaybackSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class PlaybackRestoreDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync(action) }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        assertTrue("Timed out waiting for restored playback", condition())
    }

    @Test fun serviceRestoresPausedAndSkipsDeletedUriWithoutLosingDuplicateIndex() = runBlocking {
        val container = (context.applicationContext as VinylApplication).appContainer
        onMain { context.stopService(Intent(context, PlaybackService::class.java)) }
        // Wait for old service's ordered writer to finish before seeding the new service.
        instrumentation.waitForIdleSync()
        val file = java.io.File(context.filesDir, "b3-restore-${System.nanoTime()}.wav")
        val size = 8000 * 2 * 8
        val bytes = java.nio.ByteBuffer.allocate(44 + size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray()).putInt(16)
        bytes.putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(size)
        file.writeBytes(bytes.array())
        val uri = android.net.Uri.fromFile(file).toString()
        val queue = listOf(
            PlaybackQueueItem(0, "deleted", "content://media/external/audio/media/9223372036854775807", "Deleted", "Artist", "Album"),
            PlaybackQueueItem(1, uri, uri, "Tone", "Artist", "Album"),
            PlaybackQueueItem(2, uri, uri, "Tone", "Artist", "Album"),
        )
        container.playbackDao.savePlayback(PlaybackSnapshot(currentIndex = 2, positionMs = 1700, repeatMode = 2, shuffleEnabled = true), queue, emptyList())
        val historyBefore = container.playbackDao.getPlayEvents()
        lateinit var controller: PlayerController
        try {
            onMain { controller = PlayerController(context); controller.connect() }
            await { controller.uiState.value.queue.size == 2 && controller.uiState.value.durationMs == 8000L }
            onMain { controller.updatePosition() }
            val state = controller.uiState.value
            assertEquals(1, state.currentIndex)
            assertEquals(1700L, state.positionMs)
            assertEquals(2, state.repeatMode)
            assertTrue(state.isShuffling)
            assertFalse(state.isPlaying)
            Thread.sleep(350)
            onMain { controller.updatePosition() }
            assertEquals(1700L, controller.uiState.value.positionMs)
            assertEquals(historyBefore, container.playbackDao.getPlayEvents())
            onMain { controller.removeFromQueue(1); controller.removeFromQueue(0) }
            await { controller.uiState.value.queue.isEmpty() }
        } finally {
            onMain { controller.disconnect(); context.stopService(Intent(context, PlaybackService::class.java)) }
            file.delete()
        }
    }
}
