package com.vinyl.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.shared.database.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class NowPlayingViewModel(private val controller: PlayerController) : ViewModel() {
    val uiState = controller.uiState
    init { controller.connect() }
    fun retryConnection() { controller.disconnect(); controller.connect() }
    fun playSongs(songs: List<Song>, index: Int) { runCommand { controller.playSongs(songs, index) } }
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
