package com.ysfclc.mosaic.core.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["sourceId", "sourceItemId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["kind"]),
        Index(value = ["lastPlayedAtEpochMs"]),
        Index(value = ["isBookmarked"]),
    ],
)
data class MediaItemEntity(
    @PrimaryKey val mediaId: String,
    val sourceId: String,
    val sourceItemId: String,
    val kind: String,
    val title: String,
    val creator: String? = null,
    val collectionTitle: String? = null,
    val durationMs: Long? = null,
    val mimeType: String? = null,
    val artworkUri: String? = null,
    val addedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastPlayedAtEpochMs: Long? = null,
    val resumePositionMs: Long = 0L,
    val isBookmarked: Boolean = false,
    val isHidden: Boolean = false,
    val availability: String = "UNKNOWN",
)

@Entity(
    tableName = "media_locations",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["sourceId", "uri"], unique = true),
    ],
)
data class MediaLocationEntity(
    @PrimaryKey val locationId: String,
    val mediaId: String,
    val sourceId: String,
    val uri: String,
    val mimeType: String? = null,
    val role: String = "PRIMARY",
    val hasPersistedGrant: Boolean = false,
    val availability: String = "UNKNOWN",
    val lastVerifiedAtEpochMs: Long? = null,
)

@Entity(
    tableName = "media_collections",
    indices = [Index(value = ["name"])],
)
data class MediaCollectionEntity(
    @PrimaryKey val collectionId: String,
    val name: String,
    val description: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val sortOrder: Long = 0L,
)

@Entity(
    tableName = "media_collection_items",
    primaryKeys = ["collectionId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaCollectionEntity::class,
            parentColumns = ["collectionId"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["mediaId"])],
)
data class MediaCollectionItemEntity(
    val collectionId: String,
    val mediaId: String,
    val addedAtEpochMs: Long,
    val sortOrder: Long = 0L,
)

@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["playedAtEpochMs"]),
    ],
)
data class PlaybackHistoryEntity(
    @PrimaryKey val eventId: String,
    val mediaId: String? = null,
    val titleSnapshot: String,
    val creatorSnapshot: String? = null,
    val safeUriSnapshot: String? = null,
    val playedAtEpochMs: Long,
    val resumePositionMs: Long = 0L,
)

@Entity(tableName = "playback_queue_state")
data class PlaybackQueueStateEntity(
    @PrimaryKey val queueId: String,
    val currentIndex: Int,
    val positionMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "playback_queue_entries",
    primaryKeys = ["queueId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["mediaId"])],
)
data class PlaybackQueueEntryEntity(
    val queueId: String,
    val position: Int,
    val mediaId: String? = null,
    val sourceId: String? = null,
    val titleSnapshot: String,
    val creatorSnapshot: String? = null,
    val safeUriSnapshot: String,
    val mimeType: String? = null,
)

@Entity(tableName = "migration_markers")
data class MigrationMarkerEntity(
    @PrimaryKey val markerKey: String,
    val markerValue: String,
    val completedAtEpochMs: Long,
)
