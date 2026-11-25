package com.vinyl

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader

// Android instantiates this manifest entry point.
class VinylApplication : Application(), SingletonImageLoader.Factory {
    internal lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    override fun newImageLoader(context: Context): ImageLoader = appContainer.imageLoader
}
