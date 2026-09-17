package com.usbtoolkit.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usb_toolkit_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DOWNLOAD_DIR = stringPreferencesKey("default_download_dir")
        val USB_DEST_DIR = stringPreferencesKey("default_usb_dest_dir")
        val VERIFICATION = booleanPreferencesKey("transfer_verification")
        val AUTO_USB = booleanPreferencesKey("auto_usb_detection")
        val WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.DARK.name)
            } catch (_: Exception) {
                ThemeMode.DARK
            },
            defaultDownloadDir = prefs[Keys.DOWNLOAD_DIR] ?: "",
            defaultUsbDestDir = prefs[Keys.USB_DEST_DIR] ?: "",
            transferVerificationEnabled = prefs[Keys.VERIFICATION] ?: true,
            autoUsbDetection = prefs[Keys.AUTO_USB] ?: true,
            downloadWifiOnly = prefs[Keys.WIFI_ONLY] ?: false
        )
    }

    suspend fun updateTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun updateDownloadDir(path: String) {
        context.dataStore.edit { it[Keys.DOWNLOAD_DIR] = path }
    }

    suspend fun updateUsbDestDir(path: String) {
        context.dataStore.edit { it[Keys.USB_DEST_DIR] = path }
    }

    suspend fun updateVerification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VERIFICATION] = enabled }
    }

    suspend fun updateAutoUsb(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_USB] = enabled }
    }

    suspend fun updateWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }
}
