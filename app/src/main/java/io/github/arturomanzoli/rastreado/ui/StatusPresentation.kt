package io.github.arturomanzoli.rastreado.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.ui.theme.LocalStatusPalette

val ParcelStatus.labelRes: Int
    get() = when (this) {
        ParcelStatus.UNKNOWN -> R.string.status_unknown
        ParcelStatus.PENDING -> R.string.status_pending
        ParcelStatus.WAREHOUSE -> R.string.status_warehouse
        ParcelStatus.SORTING -> R.string.status_sorting
        ParcelStatus.EXPORT_CUSTOMS -> R.string.status_export_customs
        ParcelStatus.IN_TRANSIT -> R.string.status_in_transit
        ParcelStatus.ARRIVED_DESTINATION -> R.string.status_arrived_destination
        ParcelStatus.IMPORT_CUSTOMS -> R.string.status_import_customs
        ParcelStatus.LAST_MILE -> R.string.status_last_mile
        ParcelStatus.OUT_FOR_DELIVERY -> R.string.status_out_for_delivery
        ParcelStatus.AWAITING_PICKUP -> R.string.status_awaiting_pickup
        ParcelStatus.FAILED_ATTEMPT -> R.string.status_failed_attempt
        ParcelStatus.EXCEPTION -> R.string.status_exception
        ParcelStatus.DELIVERED -> R.string.status_delivered
    }

val ParcelStatus.icon: ImageVector
    get() = when (this) {
        ParcelStatus.UNKNOWN -> Icons.AutoMirrored.Filled.HelpOutline
        ParcelStatus.PENDING -> Icons.AutoMirrored.Filled.Assignment
        ParcelStatus.WAREHOUSE -> Icons.Filled.Warehouse
        ParcelStatus.SORTING -> Icons.Filled.SwapHoriz
        ParcelStatus.EXPORT_CUSTOMS -> Icons.Filled.AirplanemodeActive
        ParcelStatus.IN_TRANSIT -> Icons.Filled.AirplanemodeActive
        ParcelStatus.ARRIVED_DESTINATION -> Icons.Filled.Flag
        ParcelStatus.IMPORT_CUSTOMS -> Icons.Filled.Gavel
        ParcelStatus.LAST_MILE -> Icons.Filled.MoveToInbox
        ParcelStatus.OUT_FOR_DELIVERY -> Icons.Filled.LocalShipping
        ParcelStatus.AWAITING_PICKUP -> Icons.Filled.Storefront
        ParcelStatus.FAILED_ATTEMPT -> Icons.Filled.ReportProblem
        ParcelStatus.EXCEPTION -> Icons.Filled.ErrorOutline
        ParcelStatus.DELIVERED -> Icons.Filled.CheckCircle
    }

@Composable
fun ParcelStatus.accent(): Color {
    val palette = LocalStatusPalette.current
    return when (this) {
        ParcelStatus.DELIVERED -> palette.delivered
        ParcelStatus.OUT_FOR_DELIVERY -> palette.outForDelivery
        ParcelStatus.IMPORT_CUSTOMS, ParcelStatus.EXPORT_CUSTOMS -> palette.customs
        ParcelStatus.FAILED_ATTEMPT, ParcelStatus.EXCEPTION -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
