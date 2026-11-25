package com.vinyl.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vinyl.shared.database.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Used on the main thread, like MediaController itself. The service outlives this connection.
internal class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var connection: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private val mutableState = MutableStateFlow(PlaybackUiState())
    val uiState = mutableState.asStateFlow()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { updateState() }
        override fun onPlayerError(error: PlaybackException) { reportError(error) }
    }

    fun connect() {
        if (connection != null) return
        val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: android.os.Bundle) {
                    updateState()
                }
                override fun onDisconnected(controller: MediaController) {
                    disconnect()
                    mutableState.value = PlaybackUiState(errorMessage = "Playback disconnected. Retry connection.")
                }
            }).buildAsync()
        connection = future
        future.addListener({
            if (connection === future) {
                try {
                    controller = future.get()
                    controller?.addListener(listener)
                    updateState()
                } catch (error: Exception) {
                    connection = null
                    reportError(error)
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun disconnect() {
        controller?.removeListener(listener)
        controller = null
        connection?.let { MediaController.releaseFuture(it) }
        connection = null
        mutableState.value = PlaybackUiState()
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        require(songs.isNotEmpty() && startIndex in songs.indices) { "Invalid playback selection" }
        val player = requireController()
        player.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0)
        player.prepare()
        player.play()
    }

    fun playPause() {
        val player = requireController()
        if (player.playWhenReady) player.pause() else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
            player.play()
        }
    }
    fun next() { requireController().seekToNextMediaItem() }
    fun previous() { requireController().seekToPrevious() }
    fun seekTo(positionMs: Long) { requireController().seekTo(positionMs.coerceAtLeast(0)) }
    fun playNext(song: Song) {
        val player = requireController()
        player.addMediaItem(if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1, song.toMediaItem())
    }
    fun addToQueue(song: Song) { requireController().addMediaItem(song.toMediaItem()) }
    fun playQueueItem(index: Int) {
        val player = requireController()
        require(index in 0 until player.mediaItemCount) { "Invalid queue index" }
        player.seekToDefaultPosition(index)
        player.prepare()
        player.play()
    }
    fun removeFromQueue(index: Int) {
        val player = requireController()
        require(index in 0 until player.mediaItemCount) { "Invalid queue index" }
        player.removeMediaItem(index)
    }
    fun setRepeatMode(mode: Int) {
        require(mode in Player.REPEAT_MODE_OFF..Player.REPEAT_MODE_ALL)
        requireController().repeatMode = mode
    }
    fun setShuffle(enabled: Boolean) { requireController().shuffleModeEnabled = enabled }
    fun reportError(error: Exception) { mutableState.value = mutableState.value.copy(errorMessage = error.message ?: "Playback failed") }

    fun updatePosition() {
        val player = controller ?: return
        mutableState.value = mutableState.value.copy(positionMs = player.currentPosition.coerceAtLeast(0))
    }

    private fun requireController(): MediaController = checkNotNull(controller) { "Player is not connected" }
    private fun updateState() {
        val player = controller ?: return
        mutableState.value = PlaybackUiState(
            queue = List(player.mediaItemCount) { player.getMediaItemAt(it) },
            currentIndex = player.currentMediaItemIndex,
            isConnected = true,
            title = player.mediaMetadata.title?.toString().orEmpty(),
            artist = player.mediaMetadata.artist?.toString().orEmpty(),
            hasMedia = player.mediaItemCount > 0,
            isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            canNext = player.hasNextMediaItem(),
            canPrevious = player.mediaItemCount > 0,
            repeatMode = player.repeatMode,
            isShuffling = player.shuffleModeEnabled,
            errorMessage = player.playerError?.localizedMessage ?: player.sessionExtras.getString("playback_storage_error"),
        )
    }
}

internal fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(contentUri).setUri(contentUri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build())
    .build()
