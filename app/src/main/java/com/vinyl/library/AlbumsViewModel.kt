package com.vinyl.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.shared.database.AlbumSummary
import com.vinyl.shared.database.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class AlbumsViewModel(
    private val repository: LibraryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AlbumsUiState())
    val uiState = mutableUiState.asStateFlow()
    private var scanJob: Job? = null
    private var albumsJob: Job? = null
    private var libraryJob: Job? = null
    private var songsJob: Job? = null
    private var hasScanned = false

    init {
        subscribeToLibrary()
    }

    private fun subscribeToLibrary() {
        if (libraryJob?.isActive != true) libraryJob = viewModelScope.launch {
            try {
                repository.observeSongs().collect { songs -> mutableUiState.update { it.copy(allSongs = songs) } }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(error = LibraryError.LOAD) }
            }
        }
        if (albumsJob?.isActive != true) albumsJob = viewModelScope.launch {
            try {
                repository.observeAlbums().collect { albums ->
                    mutableUiState.update { it.copy(albums = albums, isLoading = false) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(isLoading = false, error = LibraryError.LOAD) }
            }
        }
        if (songsJob?.isActive != true) songsJob = viewModelScope.launch {
            try {
                savedStateHandle.getStateFlow<String?>("albumId", null).flatMapLatest { albumId ->
                    mutableUiState.update { it.copy(selectedAlbumId = albumId, songs = emptyList()) }
                    if (albumId == null) flowOf(emptyList()) else repository.observeAlbumSongs(albumId)
                }.collect { songs -> mutableUiState.update { it.copy(songs = songs) } }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(error = LibraryError.LOAD) }
            }
        }
    }

    fun onAudioPermissionChanged(hasPermission: Boolean) {
        mutableUiState.update { it.copy(hasPermission = hasPermission) }
        if (!hasPermission) {
            scanJob?.cancel()
            hasScanned = false
            mutableUiState.update { it.copy(isScanning = false) }
        } else if (!hasScanned) {
            scanLibrary()
        }
    }

    fun scanLibrary() {
        if (scanJob?.isActive == true || !uiState.value.hasPermission) return
        subscribeToLibrary()
        scanJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isScanning = true, error = null) }
            try {
                repository.scanLibrary()
                hasScanned = true
            } catch (error: SecurityException) {
                mutableUiState.update { it.copy(hasPermission = false, error = LibraryError.PERMISSION) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(error = LibraryError.SCAN) }
            } finally {
                mutableUiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun onAlbumClick(albumId: String) {
        savedStateHandle["albumId"] = albumId
    }

    fun onAlbumsClick() {
        savedStateHandle["albumId"] = null
    }
}

internal enum class LibraryError { PERMISSION, SCAN, LOAD }

internal data class AlbumsUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val albums: List<AlbumSummary> = emptyList(),
    val selectedAlbumId: String? = null,
    val allSongs: List<Song> = emptyList(),
    val songs: List<Song> = emptyList(),
    val error: LibraryError? = null,
)
