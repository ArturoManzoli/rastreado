package io.github.arturomanzoli.rastreado.data.remote

import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.domain.TrackingEvent

/**
 * A source of tracking information.
 *
 * Kept as an interface because no single source covers this route reliably: Correios closed its
 * tracking API to everyone but the contracted sender, so the app leans on the forwarder's own feed
 * and needs room for alternatives without touching the rest of the codebase.
 */
interface TrackingProvider {
    val id: String

    /** [languageTag] is a BCP 47 tag, used by providers that localise their event text. */
    suspend fun fetch(code: String, languageTag: String): ProviderResult
}

data class TrackingSnapshot(
    val code: String,
    val lastMileCode: String? = null,
    val carrier: String? = null,
    val status: ParcelStatus = ParcelStatus.UNKNOWN,
    val statusDescription: String? = null,
    val originCountry: String? = null,
    val destinationCountry: String? = null,
    val progress: Float? = null,
    val events: List<TrackingEvent> = emptyList(),
)

sealed interface ProviderResult {
    data class Success(val snapshot: TrackingSnapshot) : ProviderResult

    /** The provider answered, but has no events for this code yet. */
    data class Empty(val code: String) : ProviderResult

    data class Failure(val reason: Reason, val detail: String? = null) : ProviderResult

    enum class Reason { NETWORK, SERVER, MALFORMED, UNSUPPORTED_CODE }
}
