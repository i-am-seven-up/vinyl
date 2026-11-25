package com.vinyl.library

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AlbumsViewModel : ViewModel() {
    // B1 will load real albums here; B0 does not scan or supply fake songs.
    private val mutableUiState = MutableStateFlow(AlbumsUiState())
    val uiState = mutableUiState.asStateFlow()
}

internal data class AlbumsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
