package com.vinyl.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinyl.R
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun AlbumsRoute(viewModelFactory: ViewModelProvider.Factory) {
    val albumsViewModel: AlbumsViewModel = viewModel(factory = viewModelFactory)
    val uiState by albumsViewModel.uiState.collectAsStateWithLifecycle()
    AlbumsScreen(uiState = uiState)
}

@Composable
internal fun AlbumsScreen(uiState: AlbumsUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.library_scaffold_notice))
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.library_loading))
            }
            uiState.errorMessage != null -> {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.library_next_step))
            }
        }
    }
}

@Preview(name = "Empty - Light", showBackground = true)
@Composable
private fun AlbumsEmptyLightPreview() {
    VinylTheme(isDarkTheme = false) {
        Surface { AlbumsScreen(AlbumsUiState()) }
    }
}

@Preview(name = "Empty - Dark", showBackground = true)
@Composable
private fun AlbumsEmptyDarkPreview() {
    VinylTheme(isDarkTheme = true) {
        Surface { AlbumsScreen(AlbumsUiState()) }
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun AlbumsLoadingPreview() {
    VinylTheme {
        Surface { AlbumsScreen(AlbumsUiState(isLoading = true)) }
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun AlbumsErrorPreview() {
    VinylTheme {
        Surface {
            AlbumsScreen(AlbumsUiState(errorMessage = stringResource(R.string.library_preview_error)))
        }
    }
}
