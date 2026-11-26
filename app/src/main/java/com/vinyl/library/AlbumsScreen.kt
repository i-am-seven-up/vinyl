package com.vinyl.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import com.vinyl.shared.database.Song
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.vinyl.R
import com.vinyl.shared.database.AlbumSummary
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun AlbumsRoute(viewModel: AlbumsViewModel, onPlaySongs: (List<Song>, Int) -> Unit, canPlay: Boolean, onPlayNext: (Song) -> Unit, onAddToQueue: (Song) -> Unit, findArtworkColors: suspend (ArtworkRequest) -> ArtworkColors?) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onAudioPermissionChanged(it)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAudioPermissionChanged(context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
    }
    BackHandler(enabled = state.selectedAlbumId != null) { viewModel.onAlbumsClick() }
    AlbumsScreen(
        uiState = state,
        onRequestPermission = { launcher.launch(permission) },
        onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        },
        onScan = viewModel::scanLibrary,
        onAlbumClick = viewModel::onAlbumClick,
        onAlbumsClick = viewModel::onAlbumsClick,
        onPlaySongs = onPlaySongs,
        canPlay = canPlay,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        findArtworkColors = findArtworkColors,
    )
}

@Composable
internal fun AlbumsScreen(
    uiState: AlbumsUiState,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onScan: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    canPlay: Boolean = false,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    findArtworkColors: suspend (ArtworkRequest) -> ArtworkColors? = { null },
) {
    var mode by rememberSaveable { mutableStateOf("albums") }
    var descending by rememberSaveable { mutableStateOf(false) }
    var artistId by rememberSaveable { mutableStateOf<String?>(null) }
    val sortedAlbums = remember(uiState.albums, descending) { if (descending) uiState.albums.reversed() else uiState.albums }
    val sortedSongs = remember(uiState.allSongs, descending) { if (descending) uiState.allSongs.reversed() else uiState.allSongs }
    val artists = remember(uiState.allSongs, descending) {
        val grouped = uiState.allSongs.groupBy { it.artistId }.values.sortedBy { it.first().artistNorm }
        if (descending) grouped.reversed() else grouped
    }
    BackHandler(artistId != null) { artistId = null }
    val selectedAlbum = uiState.albums.find { it.albumId == uiState.selectedAlbumId }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(selectedAlbum?.title ?: stringResource(R.string.library_title), style = MaterialTheme.typography.headlineLarge)
        if (!uiState.hasPermission) {
            Text(stringResource(R.string.library_welcome))
            Text(stringResource(R.string.library_permission))
            Button(onClick = onRequestPermission) { Text(stringResource(R.string.library_grant_permission)) }
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.library_open_settings)) }
        } else {
            Row {
                if (uiState.selectedAlbumId != null) {
                    TextButton(onClick = onAlbumsClick) { Text(stringResource(R.string.library_back)) }
                }
                TextButton(onClick = onScan, enabled = !uiState.isScanning) {
                    Text(stringResource(R.string.library_rescan))
                }
            }
            if (uiState.isLoading || uiState.isScanning) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.library_loading))
                }
            }
            if (uiState.error != null) {
                Text(stringResource(when (uiState.error) {
                    LibraryError.PERMISSION -> R.string.library_permission
                    LibraryError.SCAN -> R.string.library_scan_error
                    LibraryError.LOAD -> R.string.library_load_error
                }), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onScan, enabled = !uiState.isScanning) { Text(stringResource(R.string.library_retry)) }
            }
            if (!uiState.isLoading && !uiState.isScanning && uiState.albums.isEmpty()) {
                Text(stringResource(R.string.library_empty))
            }
            if (uiState.selectedAlbumId == null) {
                Row {
                    TextButton(onClick = { mode = "albums"; artistId = null }) { Text(stringResource(R.string.library_albums)) }
                    TextButton(onClick = { mode = "songs"; artistId = null }) { Text(stringResource(R.string.library_songs)) }
                    TextButton(onClick = { mode = "artists"; artistId = null }) { Text(stringResource(R.string.library_artists)) }
                }
                TextButton(onClick = { descending = !descending }) {
                    Text(stringResource(if (descending) R.string.library_sort_desc else R.string.library_sort_asc))
                }
                when (mode) {
                    "albums" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(sortedAlbums, key = { it.albumId }) { album -> AlbumRow(album, onAlbumClick, findArtworkColors) }
                    }
                    "songs" -> LibrarySongs(sortedSongs, canPlay, onPlaySongs, onPlayNext, onAddToQueue)
                    else -> {
                        val artistSongs = remember(sortedSongs, artistId) { sortedSongs.filter { it.artistId == artistId } }
                        if (artistId == null) LazyColumn {
                            items(artists, key = { it.first().artistId }) { songs ->
                                ListItem(headlineContent = { Text(songs.first().artist) },
                                    supportingContent = { Text(pluralStringResource(R.plurals.library_song_count, songs.size, songs.size)) },
                                    modifier = Modifier.clickable { artistId = songs.first().artistId })
                            }
                        } else {
                            TextButton(onClick = { artistId = null }) { Text(stringResource(R.string.queue_back)) }
                            LibrarySongs(artistSongs, canPlay, onPlaySongs, onPlayNext, onAddToQueue)
                        }
                    }
                }
            } else {
                if (uiState.songs.isEmpty()) Text(stringResource(R.string.library_album_empty))
                LibrarySongs(uiState.songs, canPlay, onPlaySongs, onPlayNext, onAddToQueue)
            }
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumSummary, onAlbumClick: (String) -> Unit, findArtworkColors: suspend (ArtworkRequest) -> ArtworkColors?) {
    val request = remember(album) { ArtworkRequest(album.artworkUri, album.artworkVersion, album.relativePath, album.volumeName) }
    val colors by produceState<ArtworkColors?>(null, request) { value = findArtworkColors(request) }
    Surface(color = colors?.let { Color(it.background) } ?: MaterialTheme.colorScheme.surface,
        contentColor = colors?.let { Color(it.foreground) } ?: MaterialTheme.colorScheme.onSurface) {
        Row(
            Modifier.fillMaxWidth().clickable { onAlbumClick(album.albumId) }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Text("♫", style = MaterialTheme.typography.headlineLarge)
                AsyncImage(
                    model = ArtworkRequest(album.artworkUri, album.artworkVersion, album.relativePath, album.volumeName),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column {
                Text(album.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.library_song_count, album.songCount, album.songCount))
            }
        }
    }
}

@Composable
private fun LibrarySongs(songs: List<Song>, canPlay: Boolean, onPlaySongs: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit, onAddToQueue: (Song) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(songs, key = { _, song -> song.contentUri }) { index, song ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(enabled = canPlay) { onPlaySongs(songs, index) }.padding(vertical = 8.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                    Text("${song.artist} · ${formatDuration(song.durationMs)}")
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }, enabled = canPlay) { Text(stringResource(R.string.queue_actions)) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.queue_play_next)) }, onClick = { expanded = false; onPlayNext(song) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.queue_add)) }, onClick = { expanded = false; onAddToQueue(song) })
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Preview(name = "Albums - Light", showBackground = true)
@Composable
private fun AlbumsLightPreview() {
    VinylTheme(isDarkTheme = false) {
        Surface { AlbumsScreen(AlbumsUiState(isLoading = false, hasPermission = true, albums = sampleAlbums)) }
    }
}

@Preview(name = "Albums - Dark", showBackground = true)
@Composable
private fun AlbumsDarkPreview() {
    VinylTheme(isDarkTheme = true) {
        Surface { AlbumsScreen(AlbumsUiState(isLoading = false, hasPermission = true, albums = sampleAlbums)) }
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun AlbumsEmptyPreview() {
    VinylTheme { Surface { AlbumsScreen(AlbumsUiState(isLoading = false, hasPermission = true)) } }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun AlbumsLoadingPreview() {
    VinylTheme { Surface { AlbumsScreen(AlbumsUiState(hasPermission = true, isScanning = true)) } }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun AlbumsErrorPreview() {
    VinylTheme { Surface { AlbumsScreen(AlbumsUiState(hasPermission = true, isLoading = false, error = LibraryError.SCAN)) } }
}

@Preview(name = "Permission denied", showBackground = true)
@Composable
private fun AlbumsPermissionPreview() {
    VinylTheme { Surface { AlbumsScreen(AlbumsUiState()) } }
}

private val sampleAlbums = listOf(
    AlbumSummary("external:1", "Một album có tên rất dài để kiểm tra bố cục khi hiển thị nhiều dòng", "Nghệ sĩ", 12, "", 0, null, "external"),
    AlbumSummary("external:2", "Album không có bìa", "Nghệ sĩ khác", 3, "", 0, null, "external"),
)
