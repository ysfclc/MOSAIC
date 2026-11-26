package com.ysfclc.mosaic.core.media

import android.content.Context
import com.ysfclc.mosaic.core.persistence.SafeMediaLocatorPolicy
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * A bounded, local-only snapshot of the MOSAIC-owned Media3 queue.
 *
 * Only stable, non-secret locators are persisted. Signed URLs and other locators
 * carrying query strings, fragments or user-info are deliberately omitted.
 */
data class PersistedQueueItem(
    val mediaId: String,
    val title: String,
    val creator: String?,
    val uri: String,
    val mimeType: String?,
)

data class PlaybackQueueSnapshot(
    val items: List<PersistedQueueItem>,
    val currentIndex: Int,
    val positionMs: Long,
)

object PlaybackQueueCodec {
    private const val HEADER = "mosaic-queue-v1"
    private const val FIELD_SEPARATOR = "|"
    private const val MAX_ITEMS = 100

    fun encode(snapshot: PlaybackQueueSnapshot): String {
        val safe = sanitize(snapshot)
        if (safe.items.isEmpty()) return ""
        return buildList {
            add(listOf(HEADER, safe.currentIndex.toString(), safe.positionMs.toString()).joinToString(FIELD_SEPARATOR))
            safe.items.forEach { item ->
                add(
                    listOf(
                        item.mediaId,
                        item.title,
                        item.creator.orEmpty(),
                        item.uri,
                        item.mimeType.orEmpty(),
                    ).joinToString(FIELD_SEPARATOR, transform = ::encodeField),
                )
            }
        }.joinToString("\n")
    }

    fun decode(raw: String): PlaybackQueueSnapshot? {
        val lines = raw.lineSequence().toList()
        val header = lines.firstOrNull()?.split(FIELD_SEPARATOR) ?: return null
        if (header.size != 3 || header[0] != HEADER) return null
        val selectedIndex = header[1].toIntOrNull()?.coerceAtLeast(0) ?: return null
        val positionMs = header[2].toLongOrNull()?.coerceAtLeast(0L) ?: return null

        val indexedItems = lines.drop(1).mapIndexedNotNull { index, line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != 5) return@mapIndexedNotNull null
            runCatching {
                val decoded = fields.map(::decodeField)
                PersistedQueueItem(
                    mediaId = decoded[0].ifBlank { decoded[3] },
                    title = decoded[1].ifBlank { "Untitled media" },
                    creator = decoded[2].ifBlank { null },
                    uri = decoded[3],
                    mimeType = decoded[4].ifBlank { null },
                ).takeIf(::isPersistable)?.let { index to it }
            }.getOrNull()
        }

        return sanitizeIndexed(indexedItems, selectedIndex, positionMs)
    }

    fun sanitize(snapshot: PlaybackQueueSnapshot): PlaybackQueueSnapshot =
        sanitizeIndexed(snapshot.items.mapIndexed { index, item -> index to item }, snapshot.currentIndex, snapshot.positionMs)

    private fun sanitizeIndexed(
        indexedItems: List<Pair<Int, PersistedQueueItem>>,
        selectedIndex: Int,
        positionMs: Long,
    ): PlaybackQueueSnapshot {
        val safeItems = indexedItems.filter { isPersistable(it.second) }
        if (safeItems.isEmpty()) return PlaybackQueueSnapshot(emptyList(), 0, 0L)

        val exactSelectedPosition = safeItems.indexOfFirst { it.first == selectedIndex }
        val selectedPosition = exactSelectedPosition.takeIf { it >= 0 }
            ?: safeItems.indexOfFirst { it.first > selectedIndex }.takeIf { it >= 0 }
            ?: safeItems.indexOfLast { it.first < selectedIndex }.takeIf { it >= 0 }
            ?: 0
        val windowStart = if (safeItems.size <= MAX_ITEMS) {
            0
        } else {
            (selectedPosition - MAX_ITEMS / 2).coerceIn(0, safeItems.size - MAX_ITEMS)
        }
        val bounded = safeItems.drop(windowStart).take(MAX_ITEMS)
        val boundedSelected = (selectedPosition - windowStart).coerceIn(bounded.indices)
        return PlaybackQueueSnapshot(
            items = bounded.map { (_, item) -> item },
            currentIndex = boundedSelected,
            positionMs = if (exactSelectedPosition >= 0) positionMs.coerceAtLeast(0L) else 0L,
        )
    }

    private fun isPersistable(item: PersistedQueueItem): Boolean {
        if (item.mediaId.length > 1024 || item.title.length > 512 || item.creator.orEmpty().length > 512 || item.mimeType.orEmpty().length > 128) return false
        return SafeMediaLocatorPolicy.isSafe(item.uri)
    }

    private fun encodeField(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

class PlaybackQueueStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PlaybackQueueSnapshot? =
        PlaybackQueueCodec.decode(preferences.getString(KEY_SNAPSHOT, "").orEmpty())

    fun save(snapshot: PlaybackQueueSnapshot) {
        val encoded = PlaybackQueueCodec.encode(snapshot)
        if (encoded.isBlank()) {
            clear()
        } else {
            preferences.edit().putString(KEY_SNAPSHOT, encoded).apply()
        }
    }

    fun clear() {
        preferences.edit().remove(KEY_SNAPSHOT).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "mosaic_playback_queue"
        private const val KEY_SNAPSHOT = "snapshot_v1"
    }
}
