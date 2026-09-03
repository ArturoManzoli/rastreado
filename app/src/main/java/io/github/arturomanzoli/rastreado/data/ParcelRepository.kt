package io.github.arturomanzoli.rastreado.data

import io.github.arturomanzoli.rastreado.data.local.ParcelDao
import io.github.arturomanzoli.rastreado.data.local.toDomain
import io.github.arturomanzoli.rastreado.data.local.toEntity
import io.github.arturomanzoli.rastreado.data.remote.ProviderResult
import io.github.arturomanzoli.rastreado.data.remote.TrackingProvider
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.domain.TrackingCode
import io.github.arturomanzoli.rastreado.domain.TrackingEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What a sync produced for one parcel, so callers can decide what to announce. */
data class SyncOutcome(
    val parcel: Parcel,
    val newEvents: List<TrackingEvent> = emptyList(),
    val previousStatus: ParcelStatus = ParcelStatus.UNKNOWN,
    val failure: ProviderResult.Failure? = null,
) {
    /** True when this sync is the moment the parcel became out for delivery. */
    val becameOutForDelivery: Boolean
        get() = parcel.status == ParcelStatus.OUT_FOR_DELIVERY &&
            previousStatus != ParcelStatus.OUT_FOR_DELIVERY
}

class ParcelRepository(
    private val dao: ParcelDao,
    private val provider: TrackingProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeActive(): Flow<List<Parcel>> =
        dao.observeActive().map { rows -> rows.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Parcel>> =
        dao.observeArchived().map { rows -> rows.map { it.toDomain() } }

    fun observeParcel(code: String): Flow<Parcel?> =
        dao.observeByCode(code).map { it?.toDomain() }

    fun observeEvents(code: String): Flow<List<TrackingEvent>> =
        dao.observeEvents(code).map { rows -> rows.map { it.toDomain() } }

    suspend fun exists(code: String): Boolean = dao.findByCode(TrackingCode.normalize(code)) != null

    /** Stores a parcel the user just added. Returns the normalized code it was filed under. */
    suspend fun add(rawCode: String, name: String?): String {
        val code = TrackingCode.normalize(rawCode)
        val existing = dao.findByCode(code)
        val parcel = existing?.toDomain()?.copy(name = name ?: existing.name, archived = false)
            ?: Parcel(code = code, name = name?.takeIf { it.isNotBlank() }, createdAt = now())
        dao.upsert(parcel.toEntity())
        return code
    }

    suspend fun rename(code: String, name: String?) = dao.setName(code, name?.takeIf { it.isNotBlank() })

    suspend fun setArchived(code: String, archived: Boolean) = dao.setArchived(code, archived)

    suspend fun setNotificationsEnabled(code: String, enabled: Boolean) =
        dao.setNotificationsEnabled(code, enabled)

    suspend fun setAlarmEnabled(code: String, enabled: Boolean) = dao.setAlarmEnabled(code, enabled)

    suspend fun delete(code: String) = dao.delete(code)

    /** Syncs every non-archived parcel that is not already delivered. */
    suspend fun syncAll(languageTag: String): List<SyncOutcome> =
        dao.activeParcels()
            .filterNot { it.status.isTerminal }
            .map { sync(it.code, languageTag) }

    suspend fun sync(code: String, languageTag: String): SyncOutcome {
        val stored = dao.findByCode(code)?.toDomain() ?: Parcel(code = code, createdAt = now())

        return when (val result = provider.fetch(code, languageTag)) {
            is ProviderResult.Success -> {
                val snapshot = result.snapshot
                val updated = stored.copy(
                    lastMileCode = snapshot.lastMileCode ?: stored.lastMileCode,
                    carrier = snapshot.carrier ?: stored.carrier,
                    status = snapshot.status,
                    statusDescription = snapshot.statusDescription,
                    originCountry = snapshot.originCountry ?: stored.originCountry,
                    destinationCountry = snapshot.destinationCountry ?: stored.destinationCountry,
                    progress = snapshot.progress,
                    lastEventAt = snapshot.events.firstOrNull()?.timestamp ?: stored.lastEventAt,
                    lastSyncAt = now(),
                    syncError = null,
                )
                val newEvents = dao.applySnapshot(
                    parcel = updated.toEntity(),
                    events = snapshot.events.map { it.toEntity() },
                ).map { it.toDomain() }

                SyncOutcome(
                    parcel = updated,
                    // A parcel added mid-journey would otherwise announce its whole history.
                    newEvents = if (stored.lastSyncAt == null) emptyList() else newEvents,
                    previousStatus = stored.status,
                )
            }

            is ProviderResult.Empty -> {
                val updated = stored.copy(lastSyncAt = now(), syncError = null)
                dao.upsert(updated.toEntity())
                SyncOutcome(parcel = updated, previousStatus = stored.status)
            }

            is ProviderResult.Failure -> {
                val updated = stored.copy(lastSyncAt = now(), syncError = result.reason.name)
                dao.upsert(updated.toEntity())
                SyncOutcome(parcel = updated, previousStatus = stored.status, failure = result)
            }
        }
    }

    /** Looks a code up without storing anything, so the add screen can preview it. */
    suspend fun preview(rawCode: String, languageTag: String): ProviderResult =
        provider.fetch(TrackingCode.normalize(rawCode), languageTag)
}
