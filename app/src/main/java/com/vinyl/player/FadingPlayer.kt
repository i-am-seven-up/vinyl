package com.vinyl.player

import android.os.SystemClock
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Session clients (app, notification and media buttons) all reach this player.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class FadingPlayer(private val player: Player, private val scope: CoroutineScope) : ForwardingSimpleBasePlayer(player) {
    private var fadeJob: Job? = null
    private var pendingResult: SettableFuture<Void>? = null
    private var requestedPlaying: Boolean? = null

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        cancelFade()
        requestedPlaying = playWhenReady
        val result = SettableFuture.create<Void>()
        pendingResult = result
        val startVolume = if (playWhenReady && !player.playWhenReady) 0f else player.volume
        player.volume = startVolume
        if (playWhenReady) player.play()
        if (!playWhenReady && !player.isPlaying) {
            player.pause()
            player.volume = 1f
            requestedPlaying = null
            result.set(null)
            return result
        }
        fadeJob = scope.launch {
            val start = SystemClock.elapsedRealtime()
            val target = if (playWhenReady) 1f else 0f
            var progress: Float
            do {
                progress = ((SystemClock.elapsedRealtime() - start) / 300f).coerceIn(0f, 1f)
                player.volume = startVolume + (target - startVolume) * progress
                if (progress < 1f) delay(15)
            } while (progress < 1f)
            if (!playWhenReady) player.pause()
            player.volume = 1f
            requestedPlaying = null
            result.set(null)
            pendingResult = null
        }
        return result
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        cancelFade()
        return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }

    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        cancelFade()
        return super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        cancelFade()
        return super.handleRemoveMediaItems(fromIndex, toIndex)
    }

    override fun handleStop(): ListenableFuture<*> {
        cancelFade()
        return super.handleStop()
    }

    override fun handleRelease(): ListenableFuture<*> {
        cancelFade()
        return super.handleRelease()
    }

    fun cancelFade() {
        fadeJob?.cancel()
        fadeJob = null
        if (requestedPlaying == false) player.pause()
        requestedPlaying = null
        player.volume = 1f
        pendingResult?.set(null)
        pendingResult = null
    }
}
