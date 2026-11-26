package com.vinyl.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinyl.R
import com.vinyl.shared.database.Song
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun SearchRoute(factory: ViewModelProvider.Factory, canPlay: Boolean, onPlay: (List<Song>, Int) -> Unit) {
    val model: SearchViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    SearchScreen(state, model::setQuery, model::retry, canPlay, onPlay)
}

@Composable
internal fun SearchScreen(state: SearchUiState, onQuery: (String) -> Unit, onRetry: () -> Unit, canPlay: Boolean, onPlay: (List<Song>, Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(value = state.query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_hint)) }, singleLine = true)
        when {
            state.errorMessage != null -> {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.library_retry)) }
            }
            state.isLoading -> CircularProgressIndicator()
            state.query.isBlank() -> Text(stringResource(R.string.search_start))
            state.songs.isEmpty() -> Text(stringResource(R.string.search_empty))
        }
        LazyColumn {
            itemsIndexed(state.songs, key = { _, song -> song.contentUri }) { index, song ->
                ListItem(headlineContent = { Text(song.title) }, supportingContent = { Text("${song.artist} · ${song.album}") },
                    modifier = Modifier.clickable(enabled = canPlay) { onPlay(state.songs, index) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    VinylTheme { Surface { SearchScreen(SearchUiState(query = "không có"), {}, {}, false, { _, _ -> }) } }
}

@Preview(showBackground = true)
@Composable
private fun SearchDarkPreview() {
    VinylTheme(isDarkTheme = true) { Surface { SearchScreen(SearchUiState(), {}, {}, false, { _, _ -> }) } }
}
