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
import com.vinyl.playlist.PlaylistsScreen
import com.vinyl.settings.SettingsScreen

private enum class AppDestination(val route: String, val titleRes: Int, val symbol: String) {
    LIBRARY("library", R.string.library_title, "♫"),
    PLAYLISTS("playlists", R.string.playlists_title, "≡"),
    SETTINGS("settings", R.string.settings_title, "⚙"),
}

@Composable
internal fun AppNavHost(albumsViewModelFactory: ViewModelProvider.Factory, nowPlayingViewModelFactory: ViewModelProvider.Factory) {
    val player: NowPlayingViewModel = viewModel(factory = nowPlayingViewModelFactory)
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
                AlbumsRoute(albumsViewModelFactory, player::playSongs, playback.isConnected, player::playNext, player::addToQueue)
            }
            composable("queue") {
                QueueScreen(playback, player::playQueueItem, player::removeFromQueue, { navController.popBackStack() })
            }
            composable("nowPlaying") {
                NowPlayingScreen(playback, player::playPause, player::next, player::previous, player::seekTo, player::setRepeatMode, player::setShuffle, { navController.popBackStack() }, { navController.navigate("queue") { launchSingleTop = true } })
            }
            composable(AppDestination.PLAYLISTS.route) { PlaylistsScreen() }
            composable(AppDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
