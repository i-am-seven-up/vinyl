package com.vinyl.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun PlayerPositionEffect(viewModel: NowPlayingViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.pollPosition() }
    }
}

@Composable
internal fun NowPlayingScreen(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeat: (Int) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        TextButton(onClick = onQueue) { Text(androidx.compose.ui.res.stringResource(com.vinyl.R.string.queue_title)) }
        Text("Now playing", style = MaterialTheme.typography.headlineLarge)
        Text(state.title, style = MaterialTheme.typography.titleLarge)
        Text(state.artist)
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        var seekPosition by remember { mutableStateOf<Float?>(null) }
        Slider(
            value = seekPosition ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = { seekPosition = it },
            onValueChangeFinished = { seekPosition?.let { onSeek(it.toLong()) }; seekPosition = null },
            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            enabled = state.durationMs > 0 && state.isConnected,
        )
        Text("${state.positionMs / 1000}s / ${state.durationMs / 1000}s")
        Row {
            TextButton(onClick = onPrevious, enabled = state.isConnected && state.hasMedia) { Text("Previous") }
            Button(onClick = onPlayPause, enabled = state.isConnected && state.hasMedia) { Text(if (state.isPlaying) "Pause" else "Play") }
            TextButton(onClick = onNext, enabled = state.canNext) { Text("Next") }
        }
        TextButton(onClick = { onRepeat((state.repeatMode + 1) % 3) }, enabled = state.isConnected) {
            Text("Repeat: ${listOf("Off", "One", "All")[state.repeatMode]}")
        }
        Row {
            Text("Shuffle")
            Switch(checked = state.isShuffling, onCheckedChange = onShuffle, enabled = state.isConnected)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NowPlayingPreview() {
    VinylTheme { Surface { NowPlayingScreen(PlaybackUiState(title = "Đường về", artist = "Vinyl", hasMedia = true), {}, {}, {}, {}, {}, {}, {}) } }
}
