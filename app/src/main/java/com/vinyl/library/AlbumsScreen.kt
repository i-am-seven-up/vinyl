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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.vinyl.R
import com.vinyl.shared.database.AlbumSummary
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun AlbumsRoute(viewModelFactory: ViewModelProvider.Factory, onPlaySongs: (List<com.vinyl.shared.database.Song>, Int) -> Unit, canPlay: Boolean) {
    val viewModel: AlbumsViewModel = viewModel(factory = viewModelFactory)
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
    onPlaySongs: (List<com.vinyl.shared.database.Song>, Int) -> Unit = { _, _ -> },
    canPlay: Boolean = false,
) {
    val selectedAlbum = uiState.albums.find { it.albumId == uiState.selectedAlbumId }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(selectedAlbum?.title ?: stringResource(R.string.library_title), style = MaterialTheme.typography.headlineLarge)
        if (!uiState.hasPermission) {
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.albums, key = { it.albumId }) { album ->
                        AlbumRow(album, onAlbumClick)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiState.songs.isEmpty()) {
                        item { Text(stringResource(R.string.library_album_empty)) }
                    }
                    items(uiState.songs, key = { it.contentUri }) { song ->
                        Column(Modifier.fillMaxWidth().clickable(enabled = canPlay) { onPlaySongs(uiState.songs, uiState.songs.indexOf(song)) }.padding(vertical = 8.dp)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium)
                            Text("${song.artist} · ${formatDuration(song.durationMs)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumSummary, onAlbumClick: (String) -> Unit) {
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
