package com.vinyl

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vinyl.library.AlbumsViewModel

internal class AppContainer {
    val albumsViewModelFactory = viewModelFactory {
        initializer { AlbumsViewModel() }
    }
}
