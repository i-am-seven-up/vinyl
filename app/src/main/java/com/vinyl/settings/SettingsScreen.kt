package com.vinyl.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vinyl.R
import com.vinyl.shared.ui.VinylTheme

@Composable
internal fun SettingsRoute(state: SettingsUiState, onTheme: (AppTheme) -> Unit, onScan: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED }
    SettingsScreen(state, hasPermission, onTheme, onScan, onRetry, { launcher.launch(permission) }, {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    })
}

@Composable
internal fun SettingsScreen(state: SettingsUiState, hasPermission: Boolean, onTheme: (AppTheme) -> Unit, onScan: () -> Unit,
    onRetry: () -> Unit, onPermission: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleLarge)
        if (state.isLoading) CircularProgressIndicator()
        Column(Modifier.selectableGroup()) {
            for (theme in AppTheme.entries) {
                Row(Modifier.fillMaxWidth().selectable(selected = theme == state.theme, enabled = !state.isLoading && !state.isSaving,
                    role = Role.RadioButton, onClick = { onTheme(theme) }).padding(8.dp)) {
                    RadioButton(selected = theme == state.theme, onClick = null)
                    Text(stringResource(when (theme) {
                        AppTheme.SYSTEM -> R.string.settings_system
                        AppTheme.LIGHT -> R.string.settings_light
                        AppTheme.DARK -> R.string.settings_dark
                    }), Modifier.padding(start = 12.dp))
                }
            }
        }
        if (!hasPermission) {
            Text(stringResource(R.string.library_permission))
            Button(onClick = onPermission) { Text(stringResource(R.string.library_grant_permission)) }
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.library_open_settings)) }
        }
        Button(onClick = onScan, enabled = hasPermission && !state.isScanning) { Text(stringResource(R.string.library_rescan)) }
        if (state.isScanning) {
            CircularProgressIndicator()
            Text(stringResource(R.string.library_loading))
        }
        if (state.scanCompleted) Text(stringResource(R.string.settings_scan_done))
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.library_retry)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    VinylTheme { Surface { SettingsScreen(SettingsUiState(isLoading = false), true, {}, {}, {}, {}, {}) } }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    VinylTheme(isDarkTheme = true) { Surface { SettingsScreen(SettingsUiState(theme = AppTheme.DARK, isLoading = false, errorMessage = "Không quét được nhạc"), false, {}, {}, {}, {}, {}) } }
}
