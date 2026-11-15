package com.ysfclc.mosaic.core.library

import android.content.Context
import androidx.room.withTransaction
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.persistence.MediaItemEntity
import com.ysfclc.mosaic.core.persistence.MediaItemWithLocations
import com.ysfclc.mosaic.core.persistence.MediaLocationEntity
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import com.ysfclc.mosaic.core.persistence.MosaicDatabaseProvider
import com.ysfclc.mosaic.core.persistence.SafeMediaLocatorPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Source-neutral, local-first library index.
 *
 * Adapters provide records; this repository owns canonical identity, safe
 * locations and preservation of user playback/bookmark state. It never copies
 * or mutates the underlying media files.
 */
class LibraryRepository(
    context: Context,
    private val database: MosaicDatabase = MosaicDatabaseProvider.get(context.applicationContext),
) {
    fun observeVisible(): Flow<List<MediaItemRecord>> =
        database.mediaItems().observeVisibleWithLocations().map { items ->
            items.mapNotNull { it.toMediaRecord() }
        }

    suspend fun sync(adapter: MediaSourceAdapter, query: String = "", nowEpochMs: Long = System.currentTimeMillis()): Int {
        val result = adapter.browseSnapshot(query)
        return syncRecords(
            sourceId = adapter.descriptor.id,
            records = result.items,
            nowEpochMs = nowEpochMs,
            reconcileMissing = query.isBlank() && result.isCompleteSnapshot,
        )
    }

    suspend fun syncRecords(
        sourceId: String,
        records: List<MediaItemRecord>,
        nowEpochMs: Long = System.currentTimeMillis(),
        reconcileMissing: Boolean = false,
    ): Int {
        require(sourceId.isNotBlank()) { "A media source ID is required." }
        val uniqueRecords = records
            .filter { it.sourceId == sourceId && it.uri.isNotBlank() }
            .distinctBy { it.id.ifBlank { it.uri } }
        if (uniqueRecords.isEmpty()) {
            if (reconcileMissing) {
                database.withTransaction {
                    database.mediaItems().markAllMissing(sourceId, nowEpochMs)
                    database.mediaLocations().clearForMissingSourceItems(sourceId)
                }
            }
            return 0
        }

        val sourceItemIds = uniqueRecords.map { it.id.ifBlank { it.uri } }
        val mediaIds = sourceItemIds.map { MediaIdentity.stableId(sourceId, it) }

        return database.withTransaction {
            val existingItems = sourceItemIds
                .chunked(QUERY_CHUNK_SIZE)
                .flatMap { database.mediaItems().findBySourceItems(sourceId, it) }
                .associateBy { it.sourceItemId }
            val existingLocations = mediaIds
                .chunked(QUERY_CHUNK_SIZE)
                .flatMap { database.mediaLocations().locationsForMediaIds(it) }
                .groupBy { it.mediaId }

            if (reconcileMissing) database.mediaItems().markAllMissing(sourceId, nowEpochMs)

            val itemEntities = uniqueRecords.map { record ->
                val sourceItemId = record.id.ifBlank { record.uri }
                val mediaId = MediaIdentity.stableId(sourceId, sourceItemId)
                val existing = existingItems[sourceItemId]
                val safeUri = SafeMediaLocatorPolicy.isSafe(record.uri)
                MediaItemEntity(
                    mediaId = mediaId,
                    sourceId = sourceId,
                    sourceItemId = sourceItemId,
                    kind = record.kind.name,
                    title = record.title.ifBlank { "Untitled media" },
                    creator = record.creator,
                    collectionTitle = record.collection,
                    durationMs = record.durationMs.takeIf { it > 0L },
                    mimeType = record.mimeType,
                    artworkUri = record.artworkUri?.takeIf { SafeMediaLocatorPolicy.isSafe(it) },
                    addedAtEpochMs = record.addedAtEpochSeconds
                        ?.coerceAtLeast(0L)
                        ?.times(1_000L)
                        ?: existing?.addedAtEpochMs
                        ?: nowEpochMs,
                    updatedAtEpochMs = nowEpochMs,
                    lastPlayedAtEpochMs = existing?.lastPlayedAtEpochMs,
                    resumePositionMs = existing?.resumePositionMs ?: 0L,
                    isBookmarked = existing?.isBookmarked ?: false,
                    isHidden = existing?.isHidden ?: false,
                    availability = if (safeUri) AVAILABLE else UNRESOLVED,
                )
            }
            itemEntities.chunked(QUERY_CHUNK_SIZE).forEach { database.mediaItems().upsertAll(it) }

            val locations = mutableListOf<MediaLocationEntity>()
            uniqueRecords.forEach { record ->
                val sourceItemId = record.id.ifBlank { record.uri }
                val mediaId = MediaIdentity.stableId(sourceId, sourceItemId)
                if (!SafeMediaLocatorPolicy.isSafe(record.uri)) {
                    database.mediaLocations().clearForMedia(mediaId)
                } else {
                    val previous = existingLocations[mediaId]
                        ?.firstOrNull { it.locationId == "$mediaId:primary" }
                    locations += MediaLocationEntity(
                        locationId = "$mediaId:primary",
                        mediaId = mediaId,
                        sourceId = sourceId,
                        uri = record.uri,
                        mimeType = record.mimeType,
                        role = PRIMARY_LOCATION_ROLE,
                        hasPersistedGrant = previous?.takeIf { it.uri == record.uri }?.hasPersistedGrant ?: false,
                        availability = AVAILABLE,
                        lastVerifiedAtEpochMs = nowEpochMs,
                    )
                }
            }
            locations.chunked(QUERY_CHUNK_SIZE).forEach { database.mediaLocations().upsertAll(it) }

            if (reconcileMissing) database.mediaLocations().clearForMissingSourceItems(sourceId)
            uniqueRecords.size
        }
    }

    suspend fun setBookmarked(mediaId: String, bookmarked: Boolean, nowEpochMs: Long = System.currentTimeMillis()) {
        database.mediaItems().updateBookmark(mediaId, bookmarked, nowEpochMs)
    }

    suspend fun updatePlaybackState(mediaId: String, positionMs: Long, playedAtEpochMs: Long = System.currentTimeMillis()) {
        database.mediaItems().updatePlaybackState(mediaId, positionMs.coerceAtLeast(0L), playedAtEpochMs)
    }

    private companion object {
        const val QUERY_CHUNK_SIZE = 400
        const val PRIMARY_LOCATION_ROLE = "PRIMARY"
        const val AVAILABLE = "AVAILABLE"
        const val UNRESOLVED = "UNRESOLVED"
    }
}

private fun MediaItemWithLocations.toMediaRecord(): MediaItemRecord? {
    val location = locations.firstOrNull { it.role == "PRIMARY" } ?: locations.firstOrNull() ?: return null
    val kind = runCatching { MediaKind.valueOf(item.kind) }.getOrDefault(MediaKind.AUDIO)
    return MediaItemRecord(
        id = item.sourceItemId,
        sourceId = item.sourceId,
        title = item.title,
        creator = item.creator,
        collection = item.collectionTitle,
        durationMs = item.durationMs ?: 0L,
        uri = location.uri,
        mimeType = location.mimeType ?: item.mimeType,
        artworkUri = item.artworkUri,
        kind = kind,
        addedAtEpochSeconds = item.addedAtEpochMs / 1_000L,
    )
}
