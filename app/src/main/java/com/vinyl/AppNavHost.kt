package com.vinyl

import androidx.compose.foundation.layout.padding
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
internal fun AppNavHost(albumsViewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.LIBRARY.route

    Scaffold(
        bottomBar = {
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
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.LIBRARY.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(AppDestination.LIBRARY.route) {
                AlbumsRoute(viewModelFactory = albumsViewModelFactory)
            }
            composable(AppDestination.PLAYLISTS.route) { PlaylistsScreen() }
            composable(AppDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
