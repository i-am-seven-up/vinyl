package com.vinyl.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.shared.database.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class PlaybackUiState(
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffling: Boolean = false,
    val errorMessage: String? = null,
)

internal class NowPlayingViewModel(private val controller: PlayerController) : ViewModel() {
    val uiState = controller.uiState
    init { controller.connect() }
    fun retryConnection() { controller.disconnect(); controller.connect() }
    fun playSongs(songs: List<Song>, index: Int) { runCommand { controller.playSongs(songs, index) } }
    fun playNext(song: Song) { runCommand { controller.playNext(song) } }
    fun addToQueue(song: Song) { runCommand { controller.addToQueue(song) } }
    fun removeFromQueue(index: Int) { runCommand { controller.removeFromQueue(index) } }
    fun playQueueItem(index: Int) { runCommand { controller.playQueueItem(index) } }
    fun playPause() { runCommand(controller::playPause) }
    fun next() { runCommand(controller::next) }
    fun previous() { runCommand(controller::previous) }
    fun seekTo(positionMs: Long) { runCommand { controller.seekTo(positionMs) } }
    fun setRepeatMode(mode: Int) { runCommand { controller.setRepeatMode(mode) } }
    fun setShuffle(enabled: Boolean) { runCommand { controller.setShuffle(enabled) } }
    suspend fun pollPosition() {
        while (true) { controller.updatePosition(); delay(500) }
    }
    private fun runCommand(command: () -> Unit) {
        viewModelScope.launch {
            try { command() } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { controller.reportError(error) }
        }
    }
    override fun onCleared() { controller.disconnect() }
}
