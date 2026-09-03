package io.github.arturomanzoli.rastreado.ui.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.DateFormat
import java.util.Date

/** Localised "2 hours ago" style text, which the platform already knows how to phrase. */
@Composable
fun formatRelative(epochMillis: Long): String = remember(epochMillis) {
    DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

@Composable
fun formatDateTime(epochMillis: Long): String = remember(epochMillis) {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}
