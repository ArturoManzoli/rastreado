package io.github.arturomanzoli.rastreado.domain

data class Parcel(
    val code: String,
    val name: String? = null,
    /** Last-mile code the forwarder handed to the Brazilian carrier, when known. */
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
    val notificationsEnabled: Boolean = true,
    val alarmEnabled: Boolean = true,
)

data class TrackingEvent(
    val parcelCode: String,
    val timestamp: Long,
    val actionCode: String,
    val description: String,
    val location: String? = null,
    val status: ParcelStatus = ParcelStatus.UNKNOWN,
    val carrier: String? = null,
)

data class ParcelDetails(
    val parcel: Parcel,
    val events: List<TrackingEvent>,
)
