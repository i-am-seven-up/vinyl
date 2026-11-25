package com.vinyl.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.common.Player
import com.vinyl.shared.database.Song
import org.junit.Assert.*
import org.junit.Test

internal class PlayerControllerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync(action) }

    @Test fun queueCommandsAndReconnectKeepSameSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var controller: PlayerController
        onMain { controller = PlayerController(context); controller.connect() }
        try {
            await { controller.uiState.value.isConnected }
            val songs = (1..3).map { index ->
                Song("content://media/external/audio/media/$index", "album", "artist", "Song $index", "Artist", "Album", "song", "artist", "album", 31000, index, 0, null, "external")
            }
            // Queue manipulation does not require readable media; decoder playback is a separate smoke test.
            onMain {
                controller.addToQueue(songs[0])
                controller.addToQueue(songs[2])
                controller.playNext(songs[1])
                controller.setRepeatMode(Player.REPEAT_MODE_ALL)
                controller.setShuffle(true)
            }
            await { controller.uiState.value.hasMedia && controller.uiState.value.isShuffling }
            onMain { controller.setShuffle(false); controller.next() }
            await { controller.uiState.value.title == "Song 2" }
            onMain { controller.removeFromQueue(1) }
            await { controller.uiState.value.title == "Song 3" }
            onMain { controller.addToQueue(songs[0]) }
            await { controller.uiState.value.queue.size == 3 }
            onMain { controller.playQueueItem(2) }
            await { controller.uiState.value.currentIndex == 2 }
            assertEquals("Song 1", controller.uiState.value.queue[2].mediaMetadata.title)
            onMain { controller.removeFromQueue(0) }
            await { controller.uiState.value.queue.size == 2 && controller.uiState.value.currentIndex == 1 && controller.uiState.value.title == "Song 1" }
            assertEquals("Song 1", controller.uiState.value.title)
            onMain { controller.removeFromQueue(1) }
            await { controller.uiState.value.title == "Song 3" }
            onMain { controller.disconnect(); controller.connect() }
            await { controller.uiState.value.isConnected && controller.uiState.value.title == "Song 3" }
            assertEquals(Player.REPEAT_MODE_ALL, controller.uiState.value.repeatMode)
            onMain { repeat(controller.uiState.value.queue.size) { controller.removeFromQueue(0) } }
            await { !controller.uiState.value.hasMedia }
        } finally { onMain { controller.disconnect() } }
    }

    @Test fun realAudioPlaysSeeksPausesAndAdvances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.filesDir, "b2-test-${System.nanoTime()}.wav")
        val size = 8000 * 2 * 4
        val bytes = java.nio.ByteBuffer.allocate(44 + size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray()).putInt(16)
        bytes.putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(size)
        repeat(size / 2) { bytes.putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / 8000) * 1500).toInt().toShort()) }
        file.writeBytes(bytes.array())
        val song = Song(android.net.Uri.fromFile(file).toString(), "a", "a", "Tone", "Vinyl", "Test", "tone", "vinyl", "test", 4000, 1, 0, null, "internal")
        lateinit var controller: PlayerController
        onMain { controller = PlayerController(context); controller.connect() }
        try {
            await { controller.uiState.value.isConnected }
            onMain { controller.playSongs(listOf(song, song.copy(title = "Second tone")), 0) }
            await { controller.uiState.value.durationMs == 4000L }
            await {
                onMain { controller.updatePosition() }
                controller.uiState.value.positionMs > 200
            }
            onMain { controller.playPause() }
            await { !controller.uiState.value.isPlaying }
            onMain { controller.seekTo(3000) }
            await {
                onMain { controller.updatePosition() }
                controller.uiState.value.positionMs >= 2900
            }
            onMain { controller.playPause() }
            await { controller.uiState.value.title == "Second tone" }
            assertNull(controller.uiState.value.errorMessage)
            onMain { controller.playPause(); controller.removeFromQueue(1); controller.removeFromQueue(0) }
        } finally {
            onMain { controller.disconnect() }
            file.delete()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue("Timed out waiting for MediaController state", condition())
    }
}
