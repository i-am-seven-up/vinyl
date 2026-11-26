package com.vinyl.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.shared.database.HistoryEntry
import com.vinyl.shared.database.PlaybackDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryEntry> = emptyList(),
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
)

internal class HistoryViewModel(private val dao: PlaybackDao) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HistoryUiState())
    val uiState = mutableUiState.asStateFlow()
    private var observationJob: Job? = null
    private var visibleLimit = HISTORY_PAGE_SIZE

    init { observeHistory() }

    fun retry() { observeHistory() }

    fun loadMore() {
        if (uiState.value.isLoading || !uiState.value.hasMore) return
        visibleLimit += HISTORY_PAGE_SIZE
        observeHistory()
    }

    private fun observeHistory() {
        observationJob?.cancel()
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        observationJob = viewModelScope.launch {
            try {
                dao.observeHistory(visibleLimit + 1).collect { entries ->
                    mutableUiState.update {
                        it.copy(isLoading = false, items = entries.take(visibleLimit), hasMore = entries.size > visibleLimit)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không đọc được lịch sử nghe") }
            }
        }
    }
}

private const val HISTORY_PAGE_SIZE = 100
