package io.github.arturomanzoli.rastreado.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arturomanzoli.rastreado.BuildConfig
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.data.settings.AlarmMode
import io.github.arturomanzoli.rastreado.data.settings.AppLanguage
import io.github.arturomanzoli.rastreado.data.settings.SyncInterval
import io.github.arturomanzoli.rastreado.data.settings.ThemeMode
import io.github.arturomanzoli.rastreado.ui.rememberContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = rememberContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var language by remember { mutableStateOf(viewModel.currentLanguage()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance))
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    RadioRow(
                        label = stringResource(mode.labelRes),
                        selected = settings.themeMode == mode,
                        onSelect = { viewModel.setThemeMode(mode) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_hint)) },
                    trailingContent = {
                        Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                    },
                )
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_language))
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    RadioRow(
                        label = stringResource(option.labelRes),
                        selected = language == option,
                        onSelect = {
                            language = option
                            viewModel.setLanguage(option)
                        },
                    )
                }
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_notifications))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications_enabled)) },
                trailingContent = {
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                    )
                },
            )
            SectionHeader(stringResource(R.string.settings_out_for_delivery))
            Column(Modifier.selectableGroup()) {
                AlarmMode.entries.forEach { mode ->
                    RadioRow(
                        label = stringResource(mode.labelRes),
                        selected = settings.alarmMode == mode,
                        onSelect = { viewModel.setAlarmMode(mode) },
                        enabled = settings.notificationsEnabled,
                    )
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_alarm_vibrate)) },
                trailingContent = {
                    Switch(
                        checked = settings.alarmVibrate,
                        onCheckedChange = viewModel::setAlarmVibrate,
                        enabled = settings.notificationsEnabled && settings.alarmMode == AlarmMode.ALARM,
                    )
                },
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_sync))
            Column(Modifier.selectableGroup()) {
                SyncInterval.entries.forEach { interval ->
                    RadioRow(
                        label = stringResource(interval.labelRes),
                        selected = settings.syncInterval == interval,
                        onSelect = { viewModel.setSyncInterval(interval) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_unmetered_only)) },
                supportingContent = { Text(stringResource(R.string.settings_unmetered_only_hint)) },
                trailingContent = {
                    Switch(
                        checked = settings.syncOnlyOnUnmetered,
                        onCheckedChange = viewModel::setSyncOnlyOnUnmetered,
                    )
                },
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_data_source)) },
                supportingContent = { Text(stringResource(R.string.settings_data_source_hint)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_source_code)) },
                supportingContent = { Text(stringResource(R.string.settings_source_code_hint)) },
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()),
                    )
                },
            )
        }
    }
}

private const val SOURCE_URL = "https://github.com/ArturoManzoli/rastreado"

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = onSelect, enabled = enabled) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

private val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.PORTUGUESE_BR -> R.string.language_pt_br
        AppLanguage.ENGLISH -> R.string.language_en
    }

private val AlarmMode.labelRes: Int
    get() = when (this) {
        AlarmMode.NOTIFICATION -> R.string.alarm_mode_notification
        AlarmMode.ALARM -> R.string.alarm_mode_alarm
    }

private val SyncInterval.labelRes: Int
    get() = when (this) {
        SyncInterval.QUARTER_HOUR -> R.string.sync_interval_15m
        SyncInterval.HALF_HOUR -> R.string.sync_interval_30m
        SyncInterval.HOURLY -> R.string.sync_interval_1h
        SyncInterval.THREE_HOURLY -> R.string.sync_interval_3h
        SyncInterval.SIX_HOURLY -> R.string.sync_interval_6h
    }
