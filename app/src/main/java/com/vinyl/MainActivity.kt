package com.vinyl

import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.settings.AppTheme
import com.vinyl.settings.SettingsViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vinyl.shared.ui.VinylTheme

// Android instantiates this manifest entry point.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as VinylApplication).appContainer
        setContent {
            val settings: SettingsViewModel = viewModel(factory = appContainer.settingsViewModelFactory)
            val preferences by settings.uiState.collectAsStateWithLifecycle()
            val isDark = when (preferences.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            SideEffect {
                val bars = WindowCompat.getInsetsController(window, window.decorView)
                bars.isAppearanceLightStatusBars = !isDark
                bars.isAppearanceLightNavigationBars = !isDark
            }
            VinylTheme(isDarkTheme = isDark) {
                AppNavHost(appContainer.albumsViewModelFactory, appContainer.nowPlayingViewModelFactory, appContainer.playlistsViewModelFactory, appContainer.searchViewModelFactory, settings, appContainer.artworkPalette, appContainer.historyViewModelFactory)
            }
        }
    }
}
