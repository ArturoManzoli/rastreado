package io.github.arturomanzoli.rastreado.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            themeMode = prefs.enum(THEME_MODE, ThemeMode.SYSTEM),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            syncInterval = prefs.enum(SYNC_INTERVAL, SyncInterval.HOURLY),
            notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
            alarmMode = prefs.enum(ALARM_MODE, AlarmMode.ALARM),
            alarmVibrate = prefs[ALARM_VIBRATE] ?: true,
            syncOnlyOnUnmetered = prefs[UNMETERED_ONLY] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.put(THEME_MODE, mode.name)

    suspend fun setDynamicColor(enabled: Boolean) = store.put(DYNAMIC_COLOR, enabled)

    suspend fun setSyncInterval(interval: SyncInterval) = store.put(SYNC_INTERVAL, interval.name)

    suspend fun setNotificationsEnabled(enabled: Boolean) = store.put(NOTIFICATIONS_ENABLED, enabled)

    suspend fun setAlarmMode(mode: AlarmMode) = store.put(ALARM_MODE, mode.name)

    suspend fun setAlarmVibrate(enabled: Boolean) = store.put(ALARM_VIBRATE, enabled)

    suspend fun setSyncOnlyOnUnmetered(enabled: Boolean) = store.put(UNMETERED_ONLY, enabled)

    private suspend fun <T> DataStore<Preferences>.put(key: Preferences.Key<T>, value: T) {
        edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> Preferences.enum(
        key: Preferences.Key<String>,
        default: T,
    ): T = this[key]?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SYNC_INTERVAL = stringPreferencesKey("sync_interval")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ALARM_MODE = stringPreferencesKey("alarm_mode")
        val ALARM_VIBRATE = booleanPreferencesKey("alarm_vibrate")
        val UNMETERED_ONLY = booleanPreferencesKey("unmetered_only")
    }
}
