package com.vinyl

import android.os.Bundle
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
            VinylTheme {
                AppNavHost(appContainer.albumsViewModelFactory, appContainer.nowPlayingViewModelFactory, appContainer.playlistsViewModelFactory)
            }
        }
    }
}
