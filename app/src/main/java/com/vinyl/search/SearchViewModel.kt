package com.vinyl.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.library.LibraryRepository
import com.vinyl.shared.database.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class SearchViewModel(private val repository: LibraryRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SearchUiState(query = savedState["query"] ?: ""))
    val uiState = mutableUiState.asStateFlow()
    private var searchJob: Job? = null

    init { observeResults() }

    fun setQuery(query: String) {
        if (query == uiState.value.query) return
        savedState["query"] = query
        mutableUiState.update { it.copy(query = query, songs = emptyList(), isLoading = query.isNotBlank(), errorMessage = null) }
    }

    fun retry() {
        mutableUiState.update { it.copy(isLoading = it.query.isNotBlank(), errorMessage = null) }
        observeResults()
    }

    private fun observeResults() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                savedState.getStateFlow("query", "").debounce(300).flatMapLatest { query ->
                    if (query.isBlank()) flowOf(query to emptyList())
                    else repository.observeSearchSongs(query).map { query to it }
                }.collect { (query, songs) ->
                    // A result from before the debounce must not replace a newer query.
                    if (query == uiState.value.query) mutableUiState.update { it.copy(songs = songs, isLoading = false, errorMessage = null) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không tìm được nhạc") }
            }
        }
    }
}
