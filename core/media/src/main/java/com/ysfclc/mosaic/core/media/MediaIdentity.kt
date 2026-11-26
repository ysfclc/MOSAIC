package com.ysfclc.mosaic.core.media

import java.nio.charset.StandardCharsets
import java.util.Base64

data class MediaIdentityParts(
    val sourceId: String,
    val sourceItemId: String,
)

/**
 * Creates source-scoped IDs for library/playback identity.
 *
 * A provider's local item ID is not globally unique. The source namespace is
 * encoded into the ID so unrelated adapters cannot collide in history, queues
 * or the future unified Library database.
 */
object MediaIdentity {
    private const val PREFIX = "mosaic-media-v1"

    fun forRecord(record: MediaItemRecord): String =
        stableId(record.sourceId, record.id.ifBlank { record.uri })

    fun stableId(sourceId: String, sourceItemId: String): String {
        val safeSourceId = sourceId.trim()
        val safeItemId = sourceItemId.trim()
        require(safeSourceId.isNotBlank()) { "A media source ID is required for stable identity." }
        require(safeItemId.isNotBlank()) { "A source item ID is required for stable identity." }
        return "$PREFIX:${encode(safeSourceId)}:${encode(safeItemId)}"
    }

    fun parse(stableId: String): MediaIdentityParts? {
        val fields = stableId.split(':')
        if (fields.size != 3 || fields[0] != PREFIX) return null
        return runCatching {
            MediaIdentityParts(
                sourceId = decode(fields[1]),
                sourceItemId = decode(fields[2]),
            ).takeIf { it.sourceId.isNotBlank() && it.sourceItemId.isNotBlank() }
        }.getOrNull()
    }

    fun sourceItemId(stableId: String, expectedSourceId: String): String? =
        parse(stableId)?.takeIf { it.sourceId == expectedSourceId }?.sourceItemId

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}
