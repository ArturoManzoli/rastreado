package io.github.arturomanzoli.rastreado.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.domain.TrackingEvent
import io.github.arturomanzoli.rastreado.ui.accent
import io.github.arturomanzoli.rastreado.ui.components.formatDateTime
import io.github.arturomanzoli.rastreado.ui.icon
import io.github.arturomanzoli.rastreado.ui.labelRes
import io.github.arturomanzoli.rastreado.ui.rememberContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelDetailScreen(code: String, onBack: () -> Unit) {
    val container = rememberContainer()
    val viewModel: ParcelDetailViewModel =
        viewModel(factory = ParcelDetailViewModel.factory(container, code))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }

    if (state.deleted) {
        LaunchedEffect(Unit) { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.parcel?.name?.takeIf { it.isNotBlank() } ?: code,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    val parcel = state.parcel
                    if (parcel != null) {
                        IconButton(onClick = { renaming = parcel.name.orEmpty() }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.action_rename),
                            )
                        }
                        IconButton(onClick = { viewModel.setArchived(!parcel.archived) }) {
                            Icon(
                                imageVector = if (parcel.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                contentDescription = stringResource(
                                    if (parcel.archived) R.string.action_unarchive else R.string.action_archive,
                                ),
                            )
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.parcel?.let { parcel ->
                    item { SummaryCard(parcel) }
                    item {
                        AlertPreferences(
                            parcel = parcel,
                            onNotificationsChange = viewModel::setNotificationsEnabled,
                            onAlarmChange = viewModel::setAlarmEnabled,
                        )
                    }
                }

                if (state.events.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.detail_no_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.detail_history),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    itemsIndexed(state.events, key = { _, event -> event.timestamp.toString() + event.actionCode }) { index, event ->
                        EventRow(event = event, isLatest = index == 0)
                    }
                }
            }
        }
    }

    renaming?.let { draft ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { renaming = it },
                    label = { Text(stringResource(R.string.add_name_label)) },
                    singleLine = false,
                    maxLines = 3,
                )
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rename(draft)
                        renaming = null
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_body)) },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
        )
    }
}

@Composable
private fun SummaryCard(parcel: Parcel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = parcel.status.icon,
                    contentDescription = null,
                    tint = parcel.status.accent(),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(parcel.status.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = parcel.status.accent(),
                    )
                    parcel.statusDescription?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            parcel.progress?.let { progress ->
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            DetailRow(stringResource(R.string.detail_code), parcel.code)
            parcel.lastMileCode?.let { DetailRow(stringResource(R.string.detail_last_mile_code), it) }
            parcel.carrier?.let { DetailRow(stringResource(R.string.detail_carrier), it) }
            val route = listOfNotNull(parcel.originCountry, parcel.destinationCountry)
            if (route.size == 2) {
                DetailRow(stringResource(R.string.detail_route), route.joinToString(" \u2192 "))
            }
            parcel.lastSyncAt?.let {
                DetailRow(stringResource(R.string.detail_last_checked), formatDateTime(it))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AlertPreferences(
    parcel: Parcel,
    onNotificationsChange: (Boolean) -> Unit,
    onAlarmChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.detail_notify_updates)) },
                trailingContent = {
                    Switch(checked = parcel.notificationsEnabled, onCheckedChange = onNotificationsChange)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.detail_alarm_out_for_delivery)) },
                supportingContent = { Text(stringResource(R.string.detail_alarm_out_for_delivery_hint)) },
                trailingContent = {
                    Switch(
                        checked = parcel.alarmEnabled,
                        onCheckedChange = onAlarmChange,
                        enabled = parcel.notificationsEnabled,
                    )
                },
            )
        }
    }
}

@Composable
private fun EventRow(event: TrackingEvent, isLatest: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 12.dp)
                .size(12.dp)
                .clip(CircleShape),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isLatest) event.status.accent() else MaterialTheme.colorScheme.outlineVariant,
                content = {},
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLatest) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = listOfNotNull(event.location, formatDateTime(event.timestamp)).joinToString(" \u00b7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
