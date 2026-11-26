package com.vinyl.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class FadingPlayerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync(action) }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000
        while (System.nanoTime() < deadline) {
            var success = false
            onMain { success = condition() }
            if (success) return
            Thread.sleep(10)
        }
        fail("Timed out waiting for fade/player")
    }

    @Test fun fadePauseAndInterruptedSeekNeverLeaveVolumeAtZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.filesDir, "b3-fade-${System.nanoTime()}.wav")
        val size = 8000 * 2 * 10
        val bytes = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray()).putInt(16)
        bytes.putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(size)
        file.writeBytes(bytes.array())
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Main.immediate)
        lateinit var raw: ExoPlayer
        lateinit var player: FadingPlayer
        var releasePlayer: (() -> Unit)? = null
        try {
            onMain {
                raw = ExoPlayer.Builder(context).build()
                player = FadingPlayer(raw, scope)
                releasePlayer = { player.release() }
                player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
                player.prepare()
                player.play()
            }
            await { raw.isPlaying && raw.volume == 1f }
            onMain { player.pause() }
            await { raw.volume in 0.01f..0.99f }
            await { !raw.playWhenReady && raw.volume == 1f }
            onMain { player.play() }
            await { raw.isPlaying && raw.volume == 1f }
            onMain { player.pause() }
            await { raw.volume in 0.01f..0.99f }
            onMain { player.seekTo(1000) }
            await { raw.volume == 1f && !raw.playWhenReady }
            onMain { player.play(); player.pause(); player.play() }
            await { raw.isPlaying && raw.volume == 1f }
            onMain { player.pause() }
            await { raw.volume in 0.01f..0.99f }
            onMain { player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file))); player.prepare(); player.play() }
            await { raw.isPlaying && raw.volume == 1f }
        } finally {
            onMain { releasePlayer?.invoke() }
            job.cancel()
            file.delete()
        }
    }
}
