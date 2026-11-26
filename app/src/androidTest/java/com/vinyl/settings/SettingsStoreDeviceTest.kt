package com.vinyl.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vinyl.library.LibraryRepository
import com.vinyl.shared.database.VinylDatabase
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

internal class SettingsStoreDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun themePersistsAcrossStoreReopen() = runBlocking {
        val file = File(context.cacheDir, "theme-${System.nanoTime()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun open() = SettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        try {
            var store = open()
            assertEquals(AppTheme.SYSTEM, store.observeTheme().first())
            store.setTheme(AppTheme.DARK)
            assertEquals(AppTheme.DARK, store.observeTheme().first())
            scope.coroutineContext.job.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = open()
            assertEquals(AppTheme.DARK, store.observeTheme().first())
            store.setTheme(AppTheme.LIGHT)
            assertEquals(AppTheme.LIGHT, store.observeTheme().first())
        } finally { scope.coroutineContext.job.cancelAndJoin(); file.delete() }
    }

    @Test fun settingsScanReportsFailureAndRetryCompletesWithoutImplicitScan() = runBlocking {
        val file = File(context.cacheDir, "scan-${System.nanoTime()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val db = Room.inMemoryDatabaseBuilder(context, VinylDatabase::class.java).build()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val models = ViewModelStore()
        var calls = 0
        var fail = true
        lateinit var model: SettingsViewModel
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + 5_000_000_000
            while (!condition() && System.nanoTime() < deadline) Thread.sleep(20)
            assertTrue("Settings did not settle", condition())
        }
        try {
            val store = SettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
            val repo = LibraryRepository({ calls++; if (fail) throw IOException("Scan failed"); emptyList() }, db.songDao())
            instrumentation.runOnMainSync { model = SettingsViewModel(store, repo); models.put("settings", model) }
            await { !model.uiState.value.isLoading }
            assertEquals(0, calls)
            instrumentation.runOnMainSync { model.scanLibrary() }
            await { !model.uiState.value.isScanning }
            assertTrue(model.uiState.value.hasScanError)
            assertNotNull(model.uiState.value.errorMessage)
            instrumentation.runOnMainSync { fail = false; model.retry() }
            await { !model.uiState.value.isScanning && model.uiState.value.scanCompleted }
            assertEquals(2, calls)
            assertNull(model.uiState.value.errorMessage)
        } finally {
            instrumentation.runOnMainSync { models.clear() }
            scope.coroutineContext.job.cancelAndJoin()
            db.close()
            file.delete()
        }
    }
}
