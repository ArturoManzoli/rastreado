package io.github.arturomanzoli.rastreado.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {

    @Query("SELECT * FROM parcels WHERE archived = 0 ORDER BY lastEventAt DESC, createdAt DESC")
    fun observeActive(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE archived = 1 ORDER BY lastEventAt DESC, createdAt DESC")
    fun observeArchived(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE code = :code")
    fun observeByCode(code: String): Flow<ParcelEntity?>

    @Query("SELECT * FROM parcels WHERE code = :code")
    suspend fun findByCode(code: String): ParcelEntity?

    @Query("SELECT * FROM parcels WHERE archived = 0")
    suspend fun activeParcels(): List<ParcelEntity>

    @Upsert
    suspend fun upsert(parcel: ParcelEntity)

    @Query("DELETE FROM parcels WHERE code = :code")
    suspend fun delete(code: String)

    @Query("UPDATE parcels SET archived = :archived WHERE code = :code")
    suspend fun setArchived(code: String, archived: Boolean)

    @Query("UPDATE parcels SET name = :name WHERE code = :code")
    suspend fun setName(code: String, name: String?)

    @Query("UPDATE parcels SET notificationsEnabled = :enabled WHERE code = :code")
    suspend fun setNotificationsEnabled(code: String, enabled: Boolean)

    @Query("UPDATE parcels SET alarmEnabled = :enabled WHERE code = :code")
    suspend fun setAlarmEnabled(code: String, enabled: Boolean)

    @Query("SELECT * FROM tracking_events WHERE parcelCode = :code ORDER BY timestamp DESC")
    fun observeEvents(code: String): Flow<List<TrackingEventEntity>>

    /**
     * Ignoring conflicts is what makes an event's arrival detectable: the returned row ids are
     * -1 for history already stored, so a positive id means the event is new to this device.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<TrackingEventEntity>): List<Long>

    @Transaction
    suspend fun applySnapshot(parcel: ParcelEntity, events: List<TrackingEventEntity>): List<TrackingEventEntity> {
        upsert(parcel)
        val insertedIds = insertEvents(events)
        return events.filterIndexed { index, _ -> insertedIds.getOrElse(index) { -1L } != -1L }
    }
}
