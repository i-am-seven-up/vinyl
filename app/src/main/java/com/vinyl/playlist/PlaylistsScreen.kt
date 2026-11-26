package com.vinyl.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.vinyl.R
import com.vinyl.shared.database.Song
import com.vinyl.shared.database.Playlist
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun PlaylistsRoute(factory: ViewModelProvider.Factory, canPlay: Boolean, onPlay: (List<Song>, Int) -> Unit) {
    val model: PlaylistsViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    BackHandler(state.selectedId != null) { model.selectPlaylist(null) }
    PlaylistsScreen(state, canPlay, model::selectPlaylist, model::createPlaylist, model::renamePlaylist,
        model::deletePlaylist, model::addSong, model::removeItem, model::moveItem, onPlay, model::retry, model::clearError)
}

@Composable
internal fun PlaylistsScreen(
    state: PlaylistsUiState,
    canPlay: Boolean,
    onSelect: (String?) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String, String) -> Unit,
    onMove: (String, String, Int) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    var nameDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showPicker by rememberSaveable(state.selectedId) { mutableStateOf(false) }
    var showDelete by rememberSaveable(state.selectedId) { mutableStateOf(false) }
    val selected = state.playlists.find { it.id == state.selectedId }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(selected?.name ?: stringResource(R.string.playlists_title), style = MaterialTheme.typography.headlineLarge)
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            Row {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.playlist_retry)) }
                TextButton(onClick = onClearError) { Text(stringResource(R.string.playlist_close)) }
            }
        }
        if (state.isLoading) {
            CircularProgressIndicator()
        } else if (state.selectedId == null) {
            Button(onClick = { nameDialog = "create" }, enabled = !state.isSaving) { Text(stringResource(R.string.playlist_create)) }
            if (state.playlists.isEmpty()) Text(stringResource(R.string.playlist_empty))
            LazyColumn {
                items(state.playlists, key = { it.id }) { playlist ->
                    ListItem(headlineContent = { Text(playlist.name) }, modifier = Modifier.clickable { onSelect(playlist.id) })
                }
            }
        } else {
            Row {
                TextButton(onClick = { onSelect(null) }) { Text(stringResource(R.string.playlist_back)) }
                if (selected != null) {
                    TextButton(onClick = { nameDialog = "rename" }, enabled = !state.isSaving) { Text(stringResource(R.string.playlist_rename)) }
                    TextButton(onClick = { showDelete = true }, enabled = !state.isSaving) { Text(stringResource(R.string.playlist_delete)) }
                }
            }
            if (selected == null) Text(stringResource(R.string.playlist_missing)) else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showPicker = true }, enabled = !state.isSaving) { Text(stringResource(R.string.playlist_add)) }
                    Button(onClick = { onPlay(state.songs.map { it.song }, 0) }, enabled = canPlay && state.songs.isNotEmpty()) { Text(stringResource(R.string.playlist_play)) }
                }
                if (state.songs.isEmpty()) Text(stringResource(R.string.playlist_empty_songs))
                LazyColumn {
                    itemsIndexed(state.songs, key = { _, row -> row.item.id }) { index, row ->
                        Column {
                            ListItem(headlineContent = { Text(row.song.title) }, supportingContent = { Text(row.song.artist) },
                                modifier = Modifier.clickable(enabled = canPlay) { onPlay(state.songs.map { it.song }, index) })
                            Row {
                                TextButton(onClick = { onMove(selected.id, row.item.id, -1) }, enabled = index > 0 && !state.isSaving) { Text(stringResource(R.string.playlist_up)) }
                                TextButton(onClick = { onMove(selected.id, row.item.id, 1) }, enabled = index < state.songs.lastIndex && !state.isSaving) { Text(stringResource(R.string.playlist_down)) }
                                TextButton(onClick = { onRemove(selected.id, row.item.id) }, enabled = !state.isSaving) { Text(stringResource(R.string.playlist_remove)) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (nameDialog != null) {
        PlaylistNameDialog(if (nameDialog == "rename") selected?.name.orEmpty() else "", onDismiss = { nameDialog = null }) { name ->
            if (nameDialog == "rename" && selected != null) onRename(selected.id, name) else onCreate(name)
            nameDialog = null
        }
    }
    if (showDelete && selected != null) {
        AlertDialog(onDismissRequest = { showDelete = false }, title = { Text(stringResource(R.string.playlist_delete_title)) },
            text = { Text(stringResource(R.string.playlist_delete_message, selected.name)) },
            confirmButton = { TextButton(onClick = { onDelete(selected.id); showDelete = false }) { Text(stringResource(R.string.playlist_confirm_delete)) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.playlist_cancel)) } })
    }
    if (showPicker && selected != null) {
        AlertDialog(onDismissRequest = { showPicker = false }, title = { Text(stringResource(R.string.playlist_picker)) },
            text = {
                if (state.library.isEmpty()) Text(stringResource(R.string.playlist_library_empty))
                else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(state.library, key = { it.contentUri }) { song ->
                        ListItem(headlineContent = { Text(song.title) }, supportingContent = { Text(song.artist) },
                            modifier = Modifier.clickable(enabled = !state.isSaving) { onAdd(selected.id, song.contentUri); showPicker = false })
                    }
                }
            }, confirmButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.playlist_close)) } })
    }
}

@Composable
private fun PlaylistNameDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialName.isEmpty()) stringResource(R.string.playlist_create) else stringResource(R.string.playlist_rename_title)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.playlist_name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.playlist_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.playlist_cancel)) } })
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsPreview() {
    VinylTheme { PlaylistsScreen(PlaylistsUiState(isLoading = false, playlists = listOf(Playlist("sample", "Nhạc yêu thích"))), false,
        {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { _, _, _ -> }, { _, _ -> }, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsEmptyDarkPreview() {
    VinylTheme(isDarkTheme = true) { PlaylistsScreen(PlaylistsUiState(isLoading = false), false,
        {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { _, _, _ -> }, { _, _ -> }, {}, {}) }
}
