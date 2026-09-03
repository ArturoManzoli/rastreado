package io.github.arturomanzoli.rastreado.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.github.arturomanzoli.rastreado.domain.ParcelStatus

class ParcelStatusConverter {
    @TypeConverter
    fun toStatus(name: String?): ParcelStatus =
        name?.let { runCatching { ParcelStatus.valueOf(it) }.getOrNull() } ?: ParcelStatus.UNKNOWN

    @TypeConverter
    fun fromStatus(status: ParcelStatus): String = status.name
}

@Database(
    entities = [ParcelEntity::class, TrackingEventEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ParcelStatusConverter::class)
abstract class RastreadoDatabase : RoomDatabase() {
    abstract fun parcelDao(): ParcelDao

    companion object {
        fun build(context: Context): RastreadoDatabase =
            Room.databaseBuilder(context, RastreadoDatabase::class.java, "rastreado.db").build()
    }
}
