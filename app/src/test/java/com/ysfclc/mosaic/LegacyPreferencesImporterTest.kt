package com.ysfclc.mosaic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.PersistedQueueItem
import com.ysfclc.mosaic.core.media.PlaybackQueueSnapshot
import com.ysfclc.mosaic.core.media.PlaybackQueueStore
import com.ysfclc.mosaic.core.media.PlaybackSleepTimerStore
import com.ysfclc.mosaic.core.persistence.MediaCollectionEntity
import com.ysfclc.mosaic.core.persistence.MediaCollectionItemEntity
import com.ysfclc.mosaic.core.persistence.MediaItemEntity
import com.ysfclc.mosaic.core.persistence.MediaLocationEntity
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyPreferencesImporterTest {
    @Test
    fun mirrorsLegacySnapshotsOnEveryLaunchAndRetainsPreferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearLegacyPreferences(context)
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val historyStore = PlaybackHistoryStore(context)
            val queueStore = PlaybackQueueStore(context)
            val timerStore = PlaybackSleepTimerStore(context)
            val stableId = MediaIdentity.stableId("device-library", "local:42")
            val firstHistory = PlaybackHistoryEntry(
                mediaId = stableId,
                title = "First saved track",
                creator = "Local artist",
                uri = "content://media/external/audio/media/42",
                mimeType = "audio/mpeg",
                playedAtEpochMs = 1_000L,
                resumePositionMs = 45_000L,
            )
            historyStore.record(firstHistory)
            val firstQueue = PlaybackQueueSnapshot(
                items = listOf(
                    PersistedQueueItem(stableId, "Local track", "Local artist", "content://media/external/audio/media/42", "audio/mpeg"),
                    PersistedQueueItem("podcast:episode-2", "Episode two", "Host", "https://example.com/episode-2.mp3", "audio/mpeg"),
                ),
                currentIndex = 1,
                positionMs = 321_000L,
            )
            queueStore.save(firstQueue)
            timerStore.setMinutes(15, nowEpochMs = 1_000L)

            val importer = LegacyPreferencesImporter(context, database)
            val firstImport = importer.importIfNeeded(nowEpochMs = 2_000L)

            assertFalse(firstImport.alreadyImported)
            assertEquals(1, firstImport.historyEntries)
            assertEquals(2, firstImport.queueEntries)
            assertTrue(firstImport.timerImported)
            val firstHistoryReadBack = database.playbackHistory().recent(10).single()
            assertEquals(firstHistory.title, firstHistoryReadBack.titleSnapshot)
            assertEquals(firstHistory.uri, firstHistoryReadBack.safeUriSnapshot)
            assertEquals(firstHistory.resumePositionMs, firstHistoryReadBack.resumePositionMs)
            val firstQueueState = database.playbackQueue().state("primary")
            assertEquals(1, firstQueueState?.currentIndex)
            assertEquals(321_000L, firstQueueState?.positionMs)
            val firstQueueEntries = database.playbackQueue().entries("primary")
            assertEquals(2, firstQueueEntries.size)
            assertEquals("device-library", firstQueueEntries.first().sourceId)
            assertEquals(901_000L, database.playbackTimer().state("primary")?.deadlineEpochMs)

            // The compatibility mirror keeps tracking the legacy source of truth
            // until production reads/writes are deliberately switched to Room.
            historyStore.clear()
            historyStore.record(
                firstHistory.copy(
                    mediaId = "local:99",
                    title = "Replacement track",
                    uri = "content://media/external/audio/media/99",
                    playedAtEpochMs = 3_000L,
                    resumePositionMs = 0L,
                ),
            )
            queueStore.save(
                PlaybackQueueSnapshot(
                    items = listOf(
                        PersistedQueueItem("local:99", "Replacement track", null, "content://media/external/audio/media/99", "audio/mpeg"),
                    ),
                    currentIndex = 0,
                    positionMs = 700L,
                ),
            )
            timerStore.clear()

            val secondImport = importer.importIfNeeded(nowEpochMs = 4_000L)

            assertTrue(secondImport.alreadyImported)
            assertEquals(1, database.playbackHistory().recent(10).size)
            assertEquals("Replacement track", database.playbackHistory().recent(10).single().titleSnapshot)
            assertEquals(listOf("content://media/external/audio/media/99"), database.playbackQueue().entries("primary").map { it.safeUriSnapshot })
            assertEquals(700L, database.playbackQueue().state("primary")?.positionMs)
            assertNull(database.playbackTimer().state("primary"))
            assertNotNull(database.migrationMarkers().find("legacy-playback-preferences-v1"))

            // Import never clears the old preferences; they remain available for
            // rollback until the Room-backed production cutover is verified.
            assertEquals("Replacement track", historyStore.load().single().title)
            assertEquals("content://media/external/audio/media/99", queueStore.load()?.items?.single()?.uri)
            assertEquals(0L, timerStore.deadlineEpochMs())
        } finally {
            database.close()
            clearLegacyPreferences(context)
        }
    }

    @Test
    fun parentUpsertsPreserveMediaLocationsAndCollectionMembership() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MosaicDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val media = MediaItemEntity(
                mediaId = "media-1",
                sourceId = "device-library",
                sourceItemId = "local:1",
                kind = "AUDIO",
                title = "Original title",
                addedAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
            )
            val collection = MediaCollectionEntity(
                collectionId = "collection-1",
                name = "Favorites",
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
            )
            database.mediaItems().upsert(media)
            database.mediaLocations().upsert(
                MediaLocationEntity(
                    locationId = "media-1:primary",
                    mediaId = "media-1",
                    sourceId = "device-library",
                    uri = "content://media/external/audio/media/1",
                    availability = "AVAILABLE",
                ),
            )
            database.collections().upsert(collection)
            database.collections().addItem(MediaCollectionItemEntity("collection-1", "media-1", 1L))

            database.mediaItems().upsert(media.copy(title = "Updated title", updatedAtEpochMs = 2L))
            database.collections().upsert(collection.copy(name = "Renamed favorites", updatedAtEpochMs = 2L))

            assertEquals("Updated title", database.mediaItems().findById("media-1")?.title)
            assertEquals(1, database.mediaLocations().locationsFor("media-1").size)
            assertEquals("Renamed favorites", database.collections().observeCollections().let { flow ->
                kotlinx.coroutines.flow.first(flow).single().name
            })
            assertEquals(1, database.collections().itemsInCollection("collection-1").size)
        } finally {
            database.close()
        }
    }

    private fun clearLegacyPreferences(context: Context) {
        listOf(
            "mosaic_playback_history",
            "mosaic_playback_queue",
            "mosaic_sleep_timer",
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
