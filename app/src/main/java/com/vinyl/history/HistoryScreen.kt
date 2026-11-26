package com.vinyl.history

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinyl.R
import com.vinyl.shared.database.HistoryEntry
import com.vinyl.shared.database.PlayEvent
import com.vinyl.shared.ui.VinylTheme
import java.text.DateFormat
import java.util.Date

@Composable
internal fun HistoryRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val model: HistoryViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, model::retry, model::loadMore, onBack)
}

@Composable
internal fun HistoryScreen(state: HistoryUiState, onRetry: () -> Unit, onLoadMore: () -> Unit, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.queue_back)) }
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.history_description))
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.library_retry)) }
        }
        if (state.isLoading) CircularProgressIndicator()
        if (!state.isLoading && state.errorMessage == null && state.items.isEmpty()) Text(stringResource(R.string.history_empty))
        LazyColumn {
            items(state.items, key = { it.event.id }) { entry ->
                val started = remember(entry.event.startedAtMs, dateFormat) { dateFormat.format(Date(entry.event.startedAtMs)) }
                val duration = if (entry.event.listenedMs < 1000) stringResource(R.string.history_under_second)
                    else DateUtils.formatElapsedTime(entry.event.listenedMs / 1000)
                ListItem(
                    headlineContent = { Text(entry.title ?: stringResource(R.string.history_missing_song)) },
                    supportingContent = {
                        Column {
                            Text(entry.artist ?: entry.event.mediaId)
                            Text(started)
                            Text(stringResource(R.string.history_listened, duration))
                        }
                    },
                )
            }
            if (state.hasMore && state.errorMessage == null) item {
                TextButton(onClick = onLoadMore, enabled = !state.isLoading) { Text(stringResource(R.string.history_more)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    VinylTheme { Surface {
        HistoryScreen(HistoryUiState(isLoading = false, items = listOf(
            HistoryEntry(PlayEvent("one", "content://song/1", 1_700_000_000_000, 65_000), "Đường về", "Vinyl"),
            HistoryEntry(PlayEvent("two", "content://song/2", 1_699_999_000_000, 800), null, null),
        )), {}, {}, {})
    } }
}

@Preview(showBackground = true)
@Composable
private fun HistoryEmptyDarkPreview() {
    VinylTheme(isDarkTheme = true) { Surface { HistoryScreen(HistoryUiState(isLoading = false), {}, {}, {}) } }
}
