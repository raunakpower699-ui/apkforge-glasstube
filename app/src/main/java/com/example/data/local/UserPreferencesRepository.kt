package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DownloadQuality
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.glassDataStore: DataStore<Preferences> by preferencesDataStore(name = "glasstube_prefs")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundPlayEnabled: Boolean = true,
    val backgroundDataPlayEnabled: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val dynamicIslandEnabled: Boolean = true,
    val autoSyncOnReconnect: Boolean = true,
    val defaultDownloadQuality: DownloadQuality = DownloadQuality.ULTRA_4K
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val BACKGROUND_DATA_PLAY = booleanPreferencesKey("background_data_play")
        val WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        val DYNAMIC_ISLAND = booleanPreferencesKey("dynamic_island")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
    }

    val settingsFlow: Flow<AppSettings> = context.glassDataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] }
                ?: ThemeMode.SYSTEM,
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY] ?: true,
            backgroundDataPlayEnabled = prefs[Keys.BACKGROUND_DATA_PLAY] ?: true,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY_DOWNLOAD] ?: false,
            dynamicIslandEnabled = prefs[Keys.DYNAMIC_ISLAND] ?: true,
            autoSyncOnReconnect = prefs[Keys.AUTO_SYNC] ?: true,
            defaultDownloadQuality = DownloadQuality.fromName(prefs[Keys.DEFAULT_QUALITY])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.glassDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBackgroundPlay(enabled: Boolean) {
        context.glassDataStore.edit { it[Keys.BACKGROUND_PLAY] = enabled }
    }

    suspend fun setBackgroundDataPlay(enabled: Boolean) {
        context.glassDataStore.edit { it[Keys.BACKGROUND_DATA_PLAY] = enabled }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.glassDataStore.edit { it[Keys.WIFI_ONLY_DOWNLOAD] = enabled }
    }

    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.glassDataStore.edit { it[Keys.DYNAMIC_ISLAND] = enabled }
    }

    suspend fun setAutoSyncOnReconnect(enabled: Boolean) {
        context.glassDataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun setDefaultDownloadQuality(quality: DownloadQuality) {
        context.glassDataStore.edit { it[Keys.DEFAULT_QUALITY] = quality.name }
    }
}
