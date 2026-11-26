package com.vinyl

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinyl.player.QueueScreen
import com.vinyl.player.MiniPlayer
import com.vinyl.player.NowPlayingScreen
import com.vinyl.player.NowPlayingViewModel
import com.vinyl.player.PlayerPositionEffect
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vinyl.library.AlbumsRoute
import com.vinyl.playlist.PlaylistsRoute
import com.vinyl.settings.SettingsRoute
import com.vinyl.settings.SettingsViewModel
import com.vinyl.search.SearchRoute
import com.vinyl.library.ArtworkPalette
import com.vinyl.history.HistoryRoute

private enum class AppDestination(val route: String, val titleRes: Int, val symbol: String) {
    LIBRARY("library", R.string.library_title, "♫"),
    SEARCH("search", R.string.search_title, "⌕"),
    PLAYLISTS("playlists", R.string.playlists_title, "≡"),
    SETTINGS("settings", R.string.settings_title, "⚙"),
}

@Composable
internal fun AppNavHost(albumsViewModelFactory: ViewModelProvider.Factory, nowPlayingViewModelFactory: ViewModelProvider.Factory, playlistsViewModelFactory: ViewModelProvider.Factory, searchViewModelFactory: ViewModelProvider.Factory, settings: SettingsViewModel, artworkPalette: ArtworkPalette, historyViewModelFactory: ViewModelProvider.Factory) {
    val library: com.vinyl.library.AlbumsViewModel = viewModel(factory = albumsViewModelFactory)
    val player: NowPlayingViewModel = viewModel(factory = nowPlayingViewModelFactory)
    val preferences by settings.uiState.collectAsStateWithLifecycle()
    val playback by player.uiState.collectAsStateWithLifecycle()
    PlayerPositionEffect(player)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.LIBRARY.route

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(playback, { navController.navigate("nowPlaying") { launchSingleTop = true } }, player::playPause, player::retryConnection)
                NavigationBar {
                    for (destination in AppDestination.entries) {
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.symbol) },
                            label = { Text(stringResource(destination.titleRes)) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.LIBRARY.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(AppDestination.LIBRARY.route) {
                AlbumsRoute(library, player::playSongs, playback.isConnected, player::playNext, player::addToQueue, artworkPalette::findColors)
            }
            composable("history") { HistoryRoute(historyViewModelFactory, { navController.popBackStack() }) }
            composable("queue") {
                QueueScreen(playback, player::playQueueItem, player::removeFromQueue, { navController.popBackStack() })
            }
            composable("nowPlaying") {
                NowPlayingScreen(playback, player::playPause, player::next, player::previous, player::seekTo, player::setRepeatMode, player::setShuffle, { navController.popBackStack() }, { navController.navigate("queue") { launchSingleTop = true } })
            }
            composable(AppDestination.PLAYLISTS.route) { PlaylistsRoute(playlistsViewModelFactory, playback.isConnected, player::playSongs) }
            composable(AppDestination.SEARCH.route) { SearchRoute(searchViewModelFactory, playback.isConnected, player::playSongs) }
            composable(AppDestination.SETTINGS.route) { SettingsRoute(preferences, settings::setTheme, settings::scanLibrary, settings::retry, { navController.navigate("history") { launchSingleTop = true } }) }
        }
    }
}
