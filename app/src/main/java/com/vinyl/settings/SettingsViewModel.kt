package com.vinyl.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.library.LibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isScanning: Boolean = false,
    val scanCompleted: Boolean = false,
    val errorMessage: String? = null,
    val hasScanError: Boolean = false,
)

internal class SettingsViewModel(private val store: SettingsStore, private val library: LibraryRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState = mutableUiState.asStateFlow()
    private var themeJob: Job? = null

    init { observeTheme() }

    fun retry() {
        if (uiState.value.hasScanError) scanLibrary() else {
            mutableUiState.update { it.copy(errorMessage = null) }
            observeTheme()
        }
    }

    private fun observeTheme() {
        themeJob?.cancel()
        themeJob = viewModelScope.launch {
            try {
                store.observeTheme().collect { theme -> mutableUiState.update { it.copy(theme = theme, isLoading = false) } }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không đọc được cài đặt") }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null, hasScanError = false) }
        viewModelScope.launch {
            try { store.setTheme(theme) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(errorMessage = error.message ?: "Không lưu được giao diện") }
            } finally { mutableUiState.update { it.copy(isSaving = false) } }
        }
    }

    fun scanLibrary() {
        if (uiState.value.isScanning) return
        mutableUiState.update { it.copy(isScanning = true, scanCompleted = false, errorMessage = null, hasScanError = false) }
        viewModelScope.launch {
            try {
                library.scanLibrary()
                mutableUiState.update { it.copy(scanCompleted = true) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(hasScanError = true, errorMessage = if (error is SecurityException) "Cần cấp quyền đọc âm thanh trước khi quét" else "Không quét được nhạc. Thư viện cũ được giữ nguyên.") }
            } finally { mutableUiState.update { it.copy(isScanning = false) } }
        }
    }
}
