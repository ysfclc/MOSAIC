package com.ysfclc.mosaic.core.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        MediaLocationEntity::class,
        MediaCollectionEntity::class,
        MediaCollectionItemEntity::class,
        PlaybackHistoryEntity::class,
        PlaybackQueueStateEntity::class,
        PlaybackQueueEntryEntity::class,
        MigrationMarkerEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MosaicDatabase : RoomDatabase() {
    abstract fun mediaItems(): MediaItemDao
    abstract fun mediaLocations(): MediaLocationDao
    abstract fun collections(): MediaCollectionDao
    abstract fun playbackHistory(): PlaybackHistoryDao
    abstract fun playbackQueue(): PlaybackQueueDao
    abstract fun migrationMarkers(): MigrationMarkerDao
}

object MosaicDatabaseProvider {
    @Volatile
    private var instance: MosaicDatabase? = null

    fun get(context: Context): MosaicDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MosaicDatabase::class.java,
                DATABASE_NAME,
            ).build().also { instance = it }
        }

    private const val DATABASE_NAME = "mosaic-library.db"
}
