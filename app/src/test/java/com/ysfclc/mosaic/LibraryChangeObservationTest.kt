package com.ysfclc.mosaic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ysfclc.mosaic.core.library.LibraryRepository
import com.ysfclc.mosaic.core.media.MediaBrowseResult
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.MediaSourceChange
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.media.MediaSourceDescriptor
import com.ysfclc.mosaic.core.media.ResolvedMediaSource
import com.ysfclc.mosaic.core.media.SourceCapabilities
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryChangeObservationTest {
    @Test
    fun initialSyncRunsEvenWhenAdapterHasNoChangeObserver() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val record = MediaItemRecord(
                id = "item-1",
                sourceId = "passive-source",
                title = "Initial item",
                uri = "content://media/external/audio/media/1",
                kind = MediaKind.AUDIO,
            )
            val adapter = adapterWith(record, emptyFlow())
            val counts = LibraryRepository(context, database).observeAndSync(adapter).toList()

            assertEquals(listOf(1), counts)
            assertEquals("AVAILABLE", database.mediaItems().findBySourceItems("passive-source", listOf("item-1")).single().availability)
        } finally {
            database.close()
        }
    }

    @Test
    fun adapterChangeSignalTriggersAnotherSnapshotSync() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val record = MediaItemRecord(
                id = "item-2",
                sourceId = "observed-source",
                title = "Observed item",
                uri = "content://media/external/audio/media/2",
                kind = MediaKind.AUDIO,
            )
            val counts = LibraryRepository(context, database)
                .observeAndSync(adapterWith(record, flow { emit(Unit); delay(100); emit(Unit) }))
                .toList()

            assertTrue(counts.size >= 2)
            assertEquals(1, counts.first())
            assertEquals("Observed item", database.mediaItems()
                .findBySourceItems("observed-source", listOf("item-2")).single().title)
        } finally {
            database.close()
        }
    }

    @Test
    fun itemChangeUsesTargetedLookupAndPreservesUserState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = LibraryRepository(context, database)
            val expectedItemId = "item-3"
            val initialRecord = MediaItemRecord(
                id = expectedItemId,
                sourceId = "targeted-source",
                title = "Before refresh",
                creator = "Original creator",
                uri = "content://media/external/audio/media/3",
                kind = MediaKind.AUDIO,
            )
            val changedRecord = initialRecord.copy(title = "After refresh", creator = "Updated creator")
            var snapshotQueries = 0
            var itemQueries = 0
            val adapter = object : MediaSourceAdapter {
                override val descriptor = MediaSourceDescriptor(
                    id = "targeted-source",
                    name = "Targeted source",
                    summary = "Item lookup contract test",
                    requiresNetwork = false,
                    capabilities = SourceCapabilities.LOCAL_MEDIA,
                )

                override suspend fun browse(query: String): List<MediaItemRecord> = listOf(initialRecord)

                override suspend fun browseSnapshot(query: String): MediaBrowseResult {
                    snapshotQueries++
                    return MediaBrowseResult(listOf(initialRecord), isCompleteSnapshot = true)
                }

                override fun observeChangeEvents(): Flow<MediaSourceChange> = flow {
                    delay(100)
                    emit(MediaSourceChange.ItemChanged(expectedItemId))
                }

                override suspend fun findItem(sourceItemId: String): MediaItemRecord? {
                    itemQueries++
                    return changedRecord.takeIf { sourceItemId == expectedItemId }
                }

                override suspend fun resolve(mediaId: String): ResolvedMediaSource? = null
            }
            val stableId = MediaIdentity.stableId("targeted-source", expectedItemId)
            val counts = mutableListOf<Int>()

            repository.observeAndSync(adapter).collect { count ->
                counts += count
                if (counts.size == 1) {
                    repository.setBookmarked(stableId, true, nowEpochMs = 60_000L)
                    repository.updatePlaybackState(stableId, 12_500L, playedAtEpochMs = 61_000L)
                }
            }

            assertEquals(listOf(1, 1), counts)
            assertEquals(1, snapshotQueries)
            assertEquals(1, itemQueries)
            val updated = database.mediaItems().findBySourceItems("targeted-source", listOf(expectedItemId)).single()
            assertEquals("After refresh", updated.title)
            assertEquals("Updated creator", updated.creator)
            assertTrue(updated.isBookmarked)
            assertEquals(12_500L, updated.resumePositionMs)
            assertEquals(61_000L, updated.lastPlayedAtEpochMs)
        } finally {
            database.close()
        }
    }

    @Test
    fun unavailableTargetedLookupFallsBackToIncompleteSnapshotWithoutMarkingMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = LibraryRepository(context, database)
            val expectedItemId = "item-4"
            val record = MediaItemRecord(
                id = expectedItemId,
                sourceId = "fallback-source",
                title = "Existing item",
                uri = "content://media/external/audio/media/4",
                kind = MediaKind.AUDIO,
            )
            var snapshots = 0
            val adapter = object : MediaSourceAdapter {
                override val descriptor = MediaSourceDescriptor(
                    id = "fallback-source",
                    name = "Fallback source",
                    summary = "Safe fallback contract test",
                    requiresNetwork = false,
                    capabilities = SourceCapabilities.LOCAL_MEDIA,
                )

                override suspend fun browse(query: String): List<MediaItemRecord> = listOf(record)

                override suspend fun browseSnapshot(query: String): MediaBrowseResult {
                    snapshots++
                    return if (snapshots == 1) {
                        MediaBrowseResult(listOf(record), isCompleteSnapshot = true)
                    } else {
                        MediaBrowseResult(emptyList(), isCompleteSnapshot = false)
                    }
                }

                override fun observeChangeEvents(): Flow<MediaSourceChange> = flow {
                    delay(100)
                    emit(MediaSourceChange.ItemChanged(expectedItemId))
                }

                override suspend fun findItem(sourceItemId: String): MediaItemRecord? = null

                override suspend fun resolve(mediaId: String): ResolvedMediaSource? = null
            }

            val counts = repository.observeAndSync(adapter).toList()
            assertEquals(listOf(1, 0), counts)
            assertEquals(2, snapshots)
            val existing = database.mediaItems().findBySourceItems("fallback-source", listOf(expectedItemId)).single()
            assertEquals("AVAILABLE", existing.availability)
            assertEquals(1, database.mediaLocations().locationsFor(existing.mediaId).size)
        } finally {
            database.close()
        }
    }


    @Test
    fun rapidDistinctItemChangesAreAllRefreshed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = LibraryRepository(context, database)
            val first = MediaItemRecord(
                id = "rapid-1",
                sourceId = "rapid-source",
                title = "First item",
                uri = "content://media/external/audio/media/51",
                kind = MediaKind.AUDIO,
            )
            val second = MediaItemRecord(
                id = "rapid-2",
                sourceId = "rapid-source",
                title = "Second item",
                uri = "content://media/external/audio/media/52",
                kind = MediaKind.AUDIO,
            )
            val refreshedFirst = first.copy(title = "First item refreshed")
            val refreshedSecond = second.copy(title = "Second item refreshed")
            var targetedLookups = 0
            val adapter = object : MediaSourceAdapter {
                override val descriptor = MediaSourceDescriptor(
                    id = "rapid-source",
                    name = "Rapid event source",
                    summary = "Distinct item events must not be conflated",
                    requiresNetwork = false,
                    capabilities = SourceCapabilities.LOCAL_MEDIA,
                )

                override suspend fun browse(query: String): List<MediaItemRecord> = listOf(first, second)

                override suspend fun browseSnapshot(query: String): MediaBrowseResult =
                    MediaBrowseResult(listOf(first, second), isCompleteSnapshot = true)

                override fun observeChangeEvents(): Flow<MediaSourceChange> = flow {
                    emit(MediaSourceChange.ItemChanged(first.id))
                    emit(MediaSourceChange.ItemChanged(second.id))
                }

                override suspend fun findItem(sourceItemId: String): MediaItemRecord? {
                    targetedLookups++
                    return when (sourceItemId) {
                        first.id -> refreshedFirst
                        second.id -> refreshedSecond
                        else -> null
                    }
                }

                override suspend fun resolve(mediaId: String): ResolvedMediaSource? = null
            }

            val counts = repository.observeAndSync(adapter).toList()

            assertEquals(listOf(2, 1, 1), counts)
            assertEquals(2, targetedLookups)
            val stored = database.mediaItems()
                .findBySourceItems("rapid-source", listOf(first.id, second.id))
                .associateBy { it.sourceItemId }
            assertEquals("First item refreshed", stored[first.id]?.title)
            assertEquals("Second item refreshed", stored[second.id]?.title)
        } finally {
            database.close()
        }
    }

    private fun adapterWith(record: MediaItemRecord, changes: Flow<Unit>) = object : MediaSourceAdapter {
        override val descriptor = MediaSourceDescriptor(
            id = record.sourceId,
            name = "Observation test",
            summary = "Test adapter",
            requiresNetwork = false,
            capabilities = SourceCapabilities.LOCAL_MEDIA,
        )

        override suspend fun browse(query: String): List<MediaItemRecord> = listOf(record)
        override suspend fun browseSnapshot(query: String): MediaBrowseResult =
            MediaBrowseResult(listOf(record), isCompleteSnapshot = true)
        override fun observeChanges(): Flow<Unit> = changes
        override suspend fun resolve(mediaId: String): ResolvedMediaSource? = null
    }
}
