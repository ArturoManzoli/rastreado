package io.github.arturomanzoli.rastreado.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The language options offered in settings. Not persisted here: the per-app locale is stored and
 * applied by `AppCompatDelegate`, which is the platform's own record of the override.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    PORTUGUESE_BR("pt-BR"),
    ENGLISH("en"),
}

enum class SyncInterval(val minutes: Long) {
    QUARTER_HOUR(15),
    HALF_HOUR(30),
    HOURLY(60),
    THREE_HOURLY(180),
    SIX_HOURLY(360),
}

/** How the app announces that a parcel is out for delivery. */
enum class AlarmMode {
    /** An ordinary notification on the updates channel. */
    NOTIFICATION,

    /** A ringing alarm that has to be dismissed. */
    ALARM,
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val syncInterval: SyncInterval = SyncInterval.HOURLY,
    val notificationsEnabled: Boolean = true,
    val alarmMode: AlarmMode = AlarmMode.ALARM,
    val alarmVibrate: Boolean = true,
    val syncOnlyOnUnmetered: Boolean = false,
)
