package io.github.arturomanzoli.rastreado.data.remote.cainiao

import io.github.arturomanzoli.rastreado.data.remote.TrackingSnapshot
import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.domain.StatusResolver
import io.github.arturomanzoli.rastreado.domain.TrackingCode
import io.github.arturomanzoli.rastreado.domain.TrackingEvent

/** Turns a Cainiao payload into the app's own model. Pure, so it is exercised against real JSON. */
object CainiaoMapper {

    private val LOCATION_PREFIX = Regex("""^\[([^]]{1,40})]\s*(.*)$""")

    /** Returns null when the payload carries no events, which is how Cainiao reports an unknown code. */
    fun toSnapshot(response: CainiaoResponse, requestedCode: String): TrackingSnapshot? {
        val parcel = response.module.firstOrNull { it.mailNo.equals(requestedCode, ignoreCase = true) }
            ?: response.module.firstOrNull()
            ?: return null
        if (parcel.detailList.isEmpty()) return null

        val events = parcel.detailList
            .map { it.toEvent(requestedCode, parcel.carrierName()) }
            .sortedByDescending { it.timestamp }

        return TrackingSnapshot(
            code = requestedCode,
            lastMileCode = TrackingCode.extractLastMileCode(parcel.realMailNo)
                ?.takeUnless { it.equals(requestedCode, ignoreCase = true) },
            carrier = parcel.carrierName(),
            // The newest event wins, but an unmapped code must not erase a status the history
            // already established, so fall back down the list.
            status = events.firstOrNull { it.status != ParcelStatus.UNKNOWN }?.status
                ?: ParcelStatus.UNKNOWN,
            statusDescription = events.first().description.ifBlank { parcel.statusDesc },
            originCountry = parcel.originCountry?.takeIf { it.isNotBlank() },
            destinationCountry = parcel.destCountry?.takeIf { it.isNotBlank() },
            progress = parcel.processInfo?.progressRate?.coerceIn(0f, 1f),
            events = events,
        )
    }

    private fun CainiaoParcel.carrierName(): String? =
        (combinedTrace?.currentStatusCpInfo?.cpName ?: latestTrace?.currentStatusCpInfo?.cpName)
            ?.takeIf { it.isNotBlank() }

    private fun CainiaoTrace.toEvent(parcelCode: String, fallbackCarrier: String?): TrackingEvent {
        val rawText = listOfNotNull(standerdDesc, desc).firstOrNull { it.isNotBlank() }.orEmpty()
        val match = LOCATION_PREFIX.matchEntire(rawText)
        val location = match?.groupValues?.get(1)?.trim()
        val description = match?.groupValues?.get(2)?.trim()?.ifBlank { rawText } ?: rawText

        val status = StatusResolver.resolve(actionCode, rawText)
            .takeUnless { it == ParcelStatus.UNKNOWN }
            ?: StatusResolver.fromDescription(group?.nodeDesc)
            ?: ParcelStatus.UNKNOWN

        return TrackingEvent(
            parcelCode = parcelCode,
            timestamp = time,
            actionCode = actionCode?.trim().orEmpty(),
            description = description,
            location = location,
            status = status,
            carrier = currentStatusCpInfo?.cpName?.takeIf { it.isNotBlank() } ?: fallbackCarrier,
        )
    }
}
