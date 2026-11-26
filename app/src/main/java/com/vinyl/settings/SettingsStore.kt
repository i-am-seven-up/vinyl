package com.vinyl.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

internal val Context.settingsDataStore by preferencesDataStore(name = "settings")
internal enum class AppTheme { SYSTEM, LIGHT, DARK }

internal class SettingsStore(private val dataStore: DataStore<Preferences>) {
    private val themeKey = stringPreferencesKey("theme")
    fun observeTheme() = dataStore.data.map { preferences ->
        AppTheme.entries.find { it.name == preferences[themeKey] } ?: AppTheme.SYSTEM
    }
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[themeKey] = theme.name }
    }
}
