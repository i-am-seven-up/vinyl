package com.vinyl.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vinyl.R
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun QueueScreen(
    state: PlaybackUiState,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.queue_back)) }
        Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.headlineLarge)
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.queue.isEmpty()) Text(stringResource(R.string.queue_empty))
        if (state.isShuffling) Text(stringResource(R.string.queue_order))
        LazyColumn(Modifier.weight(1f)) {
            // Indices identify occurrences: the same media ID can occur more than once.
            itemsIndexed(state.queue, key = { index, _ -> index }) { index, item ->
                val title = item.mediaMetadata.title?.toString().orEmpty()
                val isCurrent = index == state.currentIndex
                Surface(color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.weight(1f).clickable(enabled = state.isConnected) { onPlayItem(index) }.padding(8.dp)) {
                            if (isCurrent) Text(stringResource(R.string.queue_current), style = MaterialTheme.typography.labelMedium)
                            Text("${index + 1}. $title", maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.mediaMetadata.artist?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val removeDescription = stringResource(R.string.queue_remove_song, title)
                        TextButton(
                            onClick = { onRemoveItem(index) },
                            enabled = state.isConnected,
                            modifier = Modifier.semantics { contentDescription = removeDescription },
                        ) { Text(stringResource(R.string.queue_remove)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QueueEmptyPreview() {
    VinylTheme { Surface { QueueScreen(PlaybackUiState(isConnected = true), {}, {}, {}) } }
}

@Preview(showBackground = true)
@Composable
private fun QueuePreview() {
    val song = androidx.media3.common.MediaItem.Builder().setMediaId("preview")
        .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle("Đường về").setArtist("Vinyl").build()).build()
    VinylTheme { Surface { QueueScreen(PlaybackUiState(isConnected = true, queue = listOf(song, song), currentIndex = 1), {}, {}, {}) } }
}
