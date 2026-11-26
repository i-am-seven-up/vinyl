package com.vinyl.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.shared.database.*
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val playlists: List<Playlist> = emptyList(),
    val selectedId: String? = null,
    val songs: List<PlaylistSong> = emptyList(),
    val library: List<Song> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlaylistsViewModel(private val repository: PlaylistRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PlaylistsUiState())
    val uiState = mutableUiState.asStateFlow()

    private var observationJob: Job? = null

    init { observePlaylists() }

    private fun observePlaylists() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            try {
                combine(repository.observePlaylists(), repository.observeLibrary(),
                    savedState.getStateFlow<String?>("playlistId", null).flatMapLatest { id ->
                        if (id == null) flowOf(null to emptyList()) else repository.observeSongs(id).map { id to it }
                    }) { playlists, library, selected ->
                    Triple(playlists, library, selected)
                }.collect { (playlists, library, selected) ->
                    mutableUiState.update { it.copy(isLoading = false, playlists = playlists, library = library, selectedId = selected.first, songs = selected.second) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không tải được playlist") }
            }
        }
    }

    fun retry() {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        observePlaylists()
    }

    fun selectPlaylist(id: String?) { savedState["playlistId"] = id }
    fun clearError() { mutableUiState.update { it.copy(errorMessage = null) } }

    fun createPlaylist(name: String) {
        launchCommand {
            val id = UUID.randomUUID().toString()
            repository.createPlaylist(id, name)
            selectPlaylist(id)
        }
    }

    fun renamePlaylist(id: String, name: String) {
        launchCommand { repository.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: String) {
        launchCommand {
            repository.deletePlaylist(id)
            selectPlaylist(null)
        }
    }

    fun addSong(id: String, uri: String) {
        launchCommand { repository.addSong(id, uri) }
    }

    fun removeItem(id: String, itemId: String) {
        launchCommand { repository.removeItem(id, itemId) }
    }

    fun moveItem(id: String, itemId: String, offset: Int) {
        launchCommand { repository.moveItem(id, itemId, offset) }
    }

    private fun launchCommand(command: suspend () -> Unit) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try { command() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(errorMessage = error.message ?: "Không lưu được playlist") }
            } finally { mutableUiState.update { it.copy(isSaving = false) } }
        }
    }
}
