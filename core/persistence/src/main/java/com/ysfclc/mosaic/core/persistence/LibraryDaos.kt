package com.ysfclc.mosaic.core.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items WHERE mediaId = :mediaId LIMIT 1")
    suspend fun findById(mediaId: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE isHidden = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeVisible(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isBookmarked = 1 AND isHidden = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeBookmarks(): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET resumePositionMs = :positionMs, lastPlayedAtEpochMs = :playedAtEpochMs, updatedAtEpochMs = :playedAtEpochMs WHERE mediaId = :mediaId")
    suspend fun updatePlaybackState(mediaId: String, positionMs: Long, playedAtEpochMs: Long)

    @Query("UPDATE media_items SET availability = :availability, updatedAtEpochMs = :updatedAtEpochMs WHERE mediaId = :mediaId")
    suspend fun updateAvailability(mediaId: String, availability: String, updatedAtEpochMs: Long)
}

@Dao
interface MediaLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: MediaLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(locations: List<MediaLocationEntity>)

    @Query("SELECT * FROM media_locations WHERE mediaId = :mediaId ORDER BY role ASC")
    suspend fun locationsFor(mediaId: String): List<MediaLocationEntity>

    @Query("UPDATE media_locations SET availability = :availability, lastVerifiedAtEpochMs = :verifiedAtEpochMs WHERE locationId = :locationId")
    suspend fun updateAvailability(locationId: String, availability: String, verifiedAtEpochMs: Long)
}

@Dao
interface MediaCollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(collection: MediaCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(membership: MediaCollectionItemEntity)

    @Query("SELECT * FROM media_collections ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCollections(): Flow<List<MediaCollectionEntity>>

    @Query("SELECT * FROM media_collection_items WHERE collectionId = :collectionId ORDER BY sortOrder ASC, addedAtEpochMs ASC")
    suspend fun itemsInCollection(collectionId: String): List<MediaCollectionItemEntity>

    @Query("DELETE FROM media_collection_items WHERE collectionId = :collectionId AND mediaId = :mediaId")
    suspend fun removeItem(collectionId: String, mediaId: String)

    @Query("DELETE FROM media_collections WHERE collectionId = :collectionId")
    suspend fun deleteCollection(collectionId: String)
}

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(event: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY playedAtEpochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<PlaybackHistoryEntity>

    @Query("DELETE FROM playback_history WHERE playedAtEpochMs < :cutoffEpochMs")
    suspend fun pruneBefore(cutoffEpochMs: Long)
}

@Dao
interface PlaybackQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: PlaybackQueueStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PlaybackQueueEntryEntity>)

    @Query("DELETE FROM playback_queue_entries WHERE queueId = :queueId")
    suspend fun clearEntries(queueId: String)

    @Query("SELECT * FROM playback_queue_state WHERE queueId = :queueId LIMIT 1")
    suspend fun state(queueId: String): PlaybackQueueStateEntity?

    @Query("SELECT * FROM playback_queue_entries WHERE queueId = :queueId ORDER BY position ASC")
    suspend fun entries(queueId: String): List<PlaybackQueueEntryEntity>

    @Transaction
    suspend fun replaceQueue(state: PlaybackQueueStateEntity, entries: List<PlaybackQueueEntryEntity>) {
        saveState(state)
        clearEntries(state.queueId)
        if (entries.isNotEmpty()) insertEntries(entries)
    }
}

@Dao
interface MigrationMarkerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markCompleted(marker: MigrationMarkerEntity)

    @Query("SELECT * FROM migration_markers WHERE markerKey = :markerKey LIMIT 1")
    suspend fun find(markerKey: String): MigrationMarkerEntity?
}
