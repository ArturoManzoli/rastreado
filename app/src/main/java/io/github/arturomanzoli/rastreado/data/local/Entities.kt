package io.github.arturomanzoli.rastreado.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.domain.TrackingEvent

@Entity(tableName = "parcels")
data class ParcelEntity(
    @PrimaryKey val code: String,
    val name: String? = null,
    val lastMileCode: String? = null,
    val carrier: String? = null,
    val status: ParcelStatus = ParcelStatus.UNKNOWN,
    val statusDescription: String? = null,
    val originCountry: String? = null,
    val destinationCountry: String? = null,
    val progress: Float? = null,
    val lastEventAt: Long? = null,
    val lastSyncAt: Long? = null,
    val syncError: String? = null,
    val createdAt: Long = 0L,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "1") val notificationsEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1") val alarmEnabled: Boolean = true,
)

@Entity(
    tableName = "tracking_events",
    foreignKeys = [
        ForeignKey(
            entity = ParcelEntity::class,
            parentColumns = ["code"],
            childColumns = ["parcelCode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parcelCode")],
)
data class TrackingEventEntity(
    /**
     * Stable identity of an event, so re-syncing the same history is a no-op and only genuinely
     * new events can trigger a notification.
     */
    @PrimaryKey val id: String,
    val parcelCode: String,
    val timestamp: Long,
    val actionCode: String,
    val description: String,
    val location: String? = null,
    val status: ParcelStatus = ParcelStatus.UNKNOWN,
    val carrier: String? = null,
)

fun ParcelEntity.toDomain(): Parcel = Parcel(
    code = code,
    name = name,
    lastMileCode = lastMileCode,
    carrier = carrier,
    status = status,
    statusDescription = statusDescription,
    originCountry = originCountry,
    destinationCountry = destinationCountry,
    progress = progress,
    lastEventAt = lastEventAt,
    lastSyncAt = lastSyncAt,
    syncError = syncError,
    createdAt = createdAt,
    archived = archived,
    notificationsEnabled = notificationsEnabled,
    alarmEnabled = alarmEnabled,
)

fun Parcel.toEntity(): ParcelEntity = ParcelEntity(
    code = code,
    name = name,
    lastMileCode = lastMileCode,
    carrier = carrier,
    status = status,
    statusDescription = statusDescription,
    originCountry = originCountry,
    destinationCountry = destinationCountry,
    progress = progress,
    lastEventAt = lastEventAt,
    lastSyncAt = lastSyncAt,
    syncError = syncError,
    createdAt = createdAt,
    archived = archived,
    notificationsEnabled = notificationsEnabled,
    alarmEnabled = alarmEnabled,
)

fun TrackingEventEntity.toDomain(): TrackingEvent = TrackingEvent(
    parcelCode = parcelCode,
    timestamp = timestamp,
    actionCode = actionCode,
    description = description,
    location = location,
    status = status,
    carrier = carrier,
)

fun TrackingEvent.toEntity(): TrackingEventEntity = TrackingEventEntity(
    id = eventId(parcelCode, timestamp, actionCode, description),
    parcelCode = parcelCode,
    timestamp = timestamp,
    actionCode = actionCode,
    description = description,
    location = location,
    status = status,
    carrier = carrier,
)

/**
 * Includes the description because Cainiao emits distinct events that share a timestamp and an
 * action code, differing only in the city they name.
 */
private fun eventId(parcelCode: String, timestamp: Long, actionCode: String, description: String): String =
    "$parcelCode|$timestamp|$actionCode|${description.hashCode()}"
