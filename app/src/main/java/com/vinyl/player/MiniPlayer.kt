package com.vinyl.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MiniPlayer(state: PlaybackUiState, onOpen: () -> Unit, onPlayPause: () -> Unit, onRetry: () -> Unit) {
    if (state.errorMessage != null) {
        Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        if (!state.isConnected) TextButton(onClick = onRetry) { Text("Retry player connection") }
    }
    if (!state.hasMedia) return
    Surface(tonalElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(state.title, maxLines = 1)
                Text(state.artist, maxLines = 1)
            }
            TextButton(onClick = onPlayPause, enabled = state.isConnected) { Text(if (state.isPlaying) "Pause" else "Play") }
        }
    }
}
