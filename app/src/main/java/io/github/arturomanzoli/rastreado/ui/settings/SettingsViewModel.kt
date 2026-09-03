package io.github.arturomanzoli.rastreado.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.arturomanzoli.rastreado.AppContainer
import io.github.arturomanzoli.rastreado.data.settings.AlarmMode
import io.github.arturomanzoli.rastreado.data.settings.AppLanguage
import io.github.arturomanzoli.rastreado.data.settings.Settings
import io.github.arturomanzoli.rastreado.data.settings.SettingsRepository
import io.github.arturomanzoli.rastreado.data.settings.SyncInterval
import io.github.arturomanzoli.rastreado.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }

    fun setSyncInterval(interval: SyncInterval) = viewModelScope.launch { repository.setSyncInterval(interval) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }

    fun setAlarmMode(mode: AlarmMode) = viewModelScope.launch { repository.setAlarmMode(mode) }

    fun setAlarmVibrate(enabled: Boolean) = viewModelScope.launch { repository.setAlarmVibrate(enabled) }

    fun setSyncOnlyOnUnmetered(enabled: Boolean) = viewModelScope.launch {
        repository.setSyncOnlyOnUnmetered(enabled)
    }

    /** Reads the per-app locale straight from the platform, which owns this setting. */
    fun currentLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }
            ?: return AppLanguage.SYSTEM
        return when {
            tag.startsWith("pt", ignoreCase = true) -> AppLanguage.PORTUGUESE_BR
            tag.startsWith("en", ignoreCase = true) -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    fun setLanguage(language: AppLanguage) {
        val locales = language.tag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository) }
        }
    }
}
