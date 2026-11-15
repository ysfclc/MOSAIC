package com.ysfclc.mosaic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ysfclc.mosaic.core.library.LibraryRepository
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.media.MediaSourceDescriptor
import com.ysfclc.mosaic.core.media.ResolvedMediaSource
import com.ysfclc.mosaic.core.media.SourceCapabilities
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRepositoryTest {
    @Test
    fun syncsAnyAdapterPreservesUserStateAndRejectsEphemeralLocations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = LibraryRepository(context, database)
            val initialRecord = MediaItemRecord(
                id = "local:42",
                sourceId = "test-source",
                title = "Original title",
                creator = "Original artist",
                collection = "Original collection",
                durationMs = 90_000L,
                uri = "content://media/external/audio/media/42",
                mimeType = "audio/mpeg",
                artworkUri = "content://media/external/audio/albumart/8",
                kind = MediaKind.AUDIO,
                addedAtEpochSeconds = 12L,
            )
            val adapter = object : MediaSourceAdapter {
                override val descriptor = MediaSourceDescriptor(
                    id = "test-source",
                    name = "Test source",
                    summary = "Generic adapter contract test",
                    requiresNetwork = false,
                    capabilities = SourceCapabilities.LOCAL_MEDIA,
                )
                var records: List<MediaItemRecord> = listOf(initialRecord)

                override suspend fun browse(query: String): List<MediaItemRecord> = records

                override suspend fun resolve(mediaId: String): ResolvedMediaSource? = null
            }

            assertEquals(1, repository.sync(adapter, nowEpochMs = 50_000L))
            val stableId = MediaIdentity.stableId("test-source", "local:42")
            val visible = repository.observeVisible().first().single()
            assertEquals(initialRecord.title, visible.title)
            assertEquals(initialRecord.uri, visible.uri)
            assertEquals(initialRecord.id, visible.id)
            assertNotNull(database.mediaItems().findById(stableId))
            assertEquals(1, database.mediaLocations().locationsFor(stableId).size)

            repository.setBookmarked(stableId, true, nowEpochMs = 60_000L)
            repository.updatePlaybackState(stableId, 34_500L, playedAtEpochMs = 61_000L)

            adapter.records = listOf(
                initialRecord.copy(
                    title = "Updated title",
                    creator = "Updated artist",
                    uri = "content://media/external/audio/media/142",
                    addedAtEpochSeconds = 12L,
                ),
            )
            assertEquals(1, repository.sync(adapter, nowEpochMs = 70_000L))

            val updated = database.mediaItems().findById(stableId)
            assertEquals("Updated title", updated?.title)
            assertEquals(true, updated?.isBookmarked)
            assertEquals(34_500L, updated?.resumePositionMs)
            assertEquals(61_000L, updated?.lastPlayedAtEpochMs)
            assertEquals("content://media/external/audio/media/142", database.mediaLocations().locationsFor(stableId).single().uri)

            // Signed/ephemeral URLs may contribute metadata but are never persisted as locations.
            val signedRecord = adapter.records.single().copy(
                uri = "https://example.com/audio.mp3?token=secret",
            )
            assertEquals(1, repository.syncRecords("test-source", listOf(signedRecord), nowEpochMs = 80_000L))
            assertTrue(database.mediaLocations().locationsFor(stableId).isEmpty())
            assertEquals("UNRESOLVED", database.mediaItems().findById(stableId)?.availability)
            assertTrue(repository.observeVisible().first().isEmpty())

            // Identical source-local IDs from different adapters remain distinct.
            val otherSourceRecord = signedRecord.copy(
                sourceId = "another-source",
                uri = "content://media/external/audio/media/42",
            )
            assertEquals(1, repository.syncRecords("another-source", listOf(otherSourceRecord), nowEpochMs = 90_000L))
            val otherStableId = MediaIdentity.stableId("another-source", "local:42")
            assertNotEquals(stableId, otherStableId)
            assertNotNull(database.mediaItems().findById(otherStableId))
            assertEquals(1, database.mediaLocations().locationsFor(otherStableId).size)
            assertNull(database.mediaItems().findById(MediaIdentity.stableId("missing-source", "local:42")))
        } finally {
            database.close()
        }
    }
}
