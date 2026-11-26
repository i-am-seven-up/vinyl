package com.vinyl

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.room.Room
import coil3.ImageLoader
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vinyl.library.AlbumsViewModel
import com.vinyl.library.ArtworkFetcherFactory
import com.vinyl.library.ArtworkKeyer
import com.vinyl.library.ArtworkStore
import com.vinyl.library.LibraryRepository
import com.vinyl.library.MediaStoreScanner
import com.vinyl.shared.database.VinylDatabase

internal class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, VinylDatabase::class.java, "vinyl.db").addMigrations(com.vinyl.shared.database.MIGRATION_1_2, com.vinyl.shared.database.MIGRATION_2_3).build()
    internal val playbackDao = database.playbackDao()
    internal val playbackStateStore = com.vinyl.player.PlaybackStateStore(playbackDao, context.contentResolver)
    private val libraryRepository = LibraryRepository(MediaStoreScanner(context)::getSongs, database.songDao())
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ArtworkKeyer())
            add(ArtworkFetcherFactory(ArtworkStore(context.contentResolver)))
        }
        .build()

    private val playerController = com.vinyl.player.PlayerController(context)
    val nowPlayingViewModelFactory = viewModelFactory {
        initializer { com.vinyl.player.NowPlayingViewModel(playerController) }
    }
    private val playlistRepository = com.vinyl.playlist.PlaylistRepository(database.playlistDao(), database.songDao())
    val playlistsViewModelFactory = viewModelFactory {
        initializer { com.vinyl.playlist.PlaylistsViewModel(playlistRepository, createSavedStateHandle()) }
    }
    val albumsViewModelFactory = viewModelFactory {
        initializer { AlbumsViewModel(libraryRepository, createSavedStateHandle()) }
    }
}
