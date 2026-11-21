package com.ysfclc.mosaic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ysfclc.mosaic.core.library.LibraryRepository
import com.ysfclc.mosaic.core.media.MediaBrowseResult
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.media.MediaSourceDescriptor
import com.ysfclc.mosaic.core.media.ResolvedMediaSource
import com.ysfclc.mosaic.core.media.SourceCapabilities
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
                .observeAndSync(adapterWith(record, flowOf(Unit)))
                .toList()

            assertTrue(counts.size >= 1)
            assertEquals(1, counts.first())
            assertEquals("Observed item", database.mediaItems()
                .findBySourceItems("observed-source", listOf("item-2")).single().title)
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
