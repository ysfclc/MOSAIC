package com.ysfclc.mosaic

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Small local-only playback history store.
 *
 * It stores references and metadata, never media bytes. Entries are bounded and
 * malformed records are ignored so a partial/corrupt preference cannot break launch.
 */
data class PlaybackHistoryEntry(
    val mediaId: String,
    val title: String,
    val creator: String?,
    val uri: String,
    val mimeType: String?,
    val playedAtEpochMs: Long,
)

object PlaybackHistoryCodec {
    private const val FIELD_SEPARATOR = "|"
    private const val RECORD_SEPARATOR = "\n"

    fun encode(entries: List<PlaybackHistoryEntry>): String =
        entries.take(50).joinToString(RECORD_SEPARATOR) { entry ->
            listOf(
                entry.mediaId,
                entry.title,
                entry.creator.orEmpty(),
                entry.uri,
                entry.mimeType.orEmpty(),
                entry.playedAtEpochMs.toString(),
            ).joinToString(FIELD_SEPARATOR, transform = ::encodeField)
        }

    fun decode(raw: String): List<PlaybackHistoryEntry> =
        raw.lineSequence().mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != 6) return@mapNotNull null
            runCatching {
                val decoded = fields.map(::decodeField)
                PlaybackHistoryEntry(
                    mediaId = decoded[0],
                    title = decoded[1].ifBlank { "Untitled media" },
                    creator = decoded[2].ifBlank { null },
                    uri = decoded[3],
                    mimeType = decoded[4].ifBlank { null },
                    playedAtEpochMs = decoded[5].toLong(),
                ).takeIf { it.uri.isNotBlank() && it.playedAtEpochMs >= 0L }
            }.getOrNull()
        }.toList().distinctBy { it.mediaId.ifBlank { it.uri } }.take(50)

    private fun encodeField(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String =
        String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

class PlaybackHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("mosaic_playback_history", Context.MODE_PRIVATE)

    fun load(): List<PlaybackHistoryEntry> =
        PlaybackHistoryCodec.decode(preferences.getString(KEY_ENTRIES, "").orEmpty())

    fun record(entry: PlaybackHistoryEntry) {
        val updated = (listOf(entry) + load().filterNot { it.mediaId == entry.mediaId || it.uri == entry.uri }).take(50)
        preferences.edit().putString(KEY_ENTRIES, PlaybackHistoryCodec.encode(updated)).apply()
    }

    companion object {
        private const val KEY_ENTRIES = "entries_v1"
    }
}
