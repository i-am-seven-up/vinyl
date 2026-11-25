package com.vinyl

import android.app.Application

// Android instantiates this manifest entry point.
class VinylApplication : Application() {
    internal lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }
}
