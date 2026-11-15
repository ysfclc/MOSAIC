package com.ysfclc.mosaic

import android.content.Context
import androidx.room.withTransaction
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.PlaybackQueueStore
import com.ysfclc.mosaic.core.media.PlaybackSleepTimerStore
import com.ysfclc.mosaic.core.persistence.MigrationMarkerEntity
import com.ysfclc.mosaic.core.persistence.MosaicDatabase
import com.ysfclc.mosaic.core.persistence.MosaicDatabaseProvider
import com.ysfclc.mosaic.core.persistence.PlaybackHistoryEntity
import com.ysfclc.mosaic.core.persistence.PlaybackQueueEntryEntity
import com.ysfclc.mosaic.core.persistence.PlaybackQueueStateEntity
import com.ysfclc.mosaic.core.persistence.PlaybackTimerStateEntity
import com.ysfclc.mosaic.core.persistence.SafeMediaLocatorPolicy
import java.nio.charset.StandardCharsets
import java.util.UUID

data class LegacyImportSummary(
    val alreadyImported: Boolean,
    val historyEntries: Int,
    val queueEntries: Int,
    val timerImported: Boolean,
)

/**
 * Retry-safe compatibility sync of pre-Room playback preferences.
 *
 * Preferences remain the source of truth until the Room cutover. Each launch mirrors
 * the current bounded snapshot, while the marker records that initial migration ran.
 * Original preferences are never cleared; data and marker commit atomically.
 */
class LegacyPreferencesImporter(
    context: Context,
    private val database: MosaicDatabase = MosaicDatabaseProvider.get(context.applicationContext),
) {
    private val appContext = context.applicationContext

    suspend fun importIfNeeded(nowEpochMs: Long = System.currentTimeMillis()): LegacyImportSummary =
        database.withTransaction {
            val markerDao = database.migrationMarkers()
            val alreadyImported = markerDao.find(MARKER_KEY) != null
            val history = PlaybackHistoryStore(appContext).load()
            val queue = PlaybackQueueStore(appContext).load()
            val timerDeadline = PlaybackSleepTimerStore(appContext).deadlineEpochMs()

            val historyEntities = history.map { entry ->
                val linkedMediaId = database.mediaItems().findById(entry.mediaId)?.mediaId
                PlaybackHistoryEntity(
                    eventId = legacyEventId(entry.mediaId, entry.uri, entry.playedAtEpochMs),
                    mediaId = linkedMediaId,
                    titleSnapshot = entry.title,
                    creatorSnapshot = entry.creator,
                    safeUriSnapshot = entry.uri.takeIf { SafeMediaLocatorPolicy.isSafe(it) },
                    playedAtEpochMs = entry.playedAtEpochMs,
                    resumePositionMs = entry.resumePositionMs.coerceAtLeast(0L),
                )
            }
            database.playbackHistory().clearAll()
            historyEntities.forEach { database.playbackHistory().record(it) }

            val queueEntries = queue?.items.orEmpty().mapIndexed { index, item ->
                val linkedMediaId = database.mediaItems().findById(item.mediaId)?.mediaId
                PlaybackQueueEntryEntity(
                    queueId = PRIMARY_QUEUE_ID,
                    position = index,
                    mediaId = linkedMediaId,
                    sourceId = MediaIdentity.parse(item.mediaId)?.sourceId,
                    titleSnapshot = item.title,
                    creatorSnapshot = item.creator,
                    safeUriSnapshot = item.uri,
                    mimeType = item.mimeType,
                )
            }
            if (queue != null && queueEntries.isNotEmpty()) {
                database.playbackQueue().replaceQueue(
                    PlaybackQueueStateEntity(
                        queueId = PRIMARY_QUEUE_ID,
                        currentIndex = queue.currentIndex.coerceIn(queueEntries.indices),
                        positionMs = queue.positionMs.coerceAtLeast(0L),
                        updatedAtEpochMs = nowEpochMs,
                    ),
                    queueEntries,
                )
            } else {
                database.playbackQueue().clearQueue(PRIMARY_QUEUE_ID)
            }

            val timerImported = timerDeadline > 0L
            if (timerImported) {
                database.playbackTimer().save(
                    PlaybackTimerStateEntity(
                        timerId = PRIMARY_TIMER_ID,
                        deadlineEpochMs = timerDeadline,
                        updatedAtEpochMs = nowEpochMs,
                    ),
                )
            } else {
                database.playbackTimer().clear(PRIMARY_TIMER_ID)
            }

            // Verify the transactional view before writing the initial marker. Any failed
            // check rolls back the entire transaction and leaves the next launch retryable.
            check(historyEntities.all { database.playbackHistory().findByEventId(it.eventId) != null }) {
                "Legacy playback history import read-back did not match."
            }

            if (queue != null && queueEntries.isNotEmpty()) {
                val restoredState = database.playbackQueue().state(PRIMARY_QUEUE_ID)
                val restoredEntries = database.playbackQueue().entries(PRIMARY_QUEUE_ID)
                check(restoredState?.currentIndex == queue.currentIndex.coerceIn(queueEntries.indices))
                check(restoredState?.positionMs == queue.positionMs.coerceAtLeast(0L))
                check(restoredEntries == queueEntries)
            } else {
                check(database.playbackQueue().state(PRIMARY_QUEUE_ID) == null)
                check(database.playbackQueue().entries(PRIMARY_QUEUE_ID).isEmpty())
            }

            if (timerImported) {
                check(database.playbackTimer().state(PRIMARY_TIMER_ID)?.deadlineEpochMs == timerDeadline)
            } else {
                check(database.playbackTimer().state(PRIMARY_TIMER_ID) == null)
            }

            if (!alreadyImported) {
                markerDao.markCompleted(
                    MigrationMarkerEntity(
                        markerKey = MARKER_KEY,
                        markerValue = "history=${historyEntities.size};queue=${queueEntries.size};timer=$timerImported",
                        completedAtEpochMs = nowEpochMs,
                    ),
                )
            }

            LegacyImportSummary(
                alreadyImported = alreadyImported,
                historyEntries = historyEntities.size,
                queueEntries = queueEntries.size,
                timerImported = timerImported,
            )
        }

    private fun legacyEventId(mediaId: String, uri: String, playedAtEpochMs: Long): String {
        val seed = "$mediaId\u0000$uri\u0000$playedAtEpochMs"
        return "legacy-history:${UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8))}"
    }

    private companion object {
        const val MARKER_KEY = "legacy-playback-preferences-v1"
        const val PRIMARY_QUEUE_ID = "primary"
        const val PRIMARY_TIMER_ID = "primary"
    }
}
