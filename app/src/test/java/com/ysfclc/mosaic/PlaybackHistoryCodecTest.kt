package com.ysfclc.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHistoryCodecTest {
    @Test
    fun roundTripsUnicodeAndOptionalFields() {
        val entries = listOf(
            PlaybackHistoryEntry(
                mediaId = "podcast:episode-1",
                title = "Zażółć gęślą jaźń 🎧",
                creator = "MOSAIC / Test",
                uri = "https://example.com/audio?token=a|b",
                mimeType = "audio/mpeg",
                playedAtEpochMs = 1_794_600_000_000L,
                resumePositionMs = 245_000L,
            ),
            PlaybackHistoryEntry(
                mediaId = "local:42",
                title = "Untitled media",
                creator = null,
                uri = "content://media/external/audio/media/42",
                mimeType = null,
                playedAtEpochMs = 1_794_600_000_100L,
            ),
        )

        assertEquals(entries, PlaybackHistoryCodec.decode(PlaybackHistoryCodec.encode(entries)))
    }

    @Test
    fun ignoresMalformedRowsAndRejectsInvalidReferences() {
        val valid = PlaybackHistoryCodec.encode(
            listOf(PlaybackHistoryEntry("one", "One", null, "https://example.com/one.mp3", null, 10L)),
        )
        val decoded = PlaybackHistoryCodec.decode("$valid\nnot-a-valid-row\n")

        assertEquals(1, decoded.size)
        assertEquals("one", decoded.single().mediaId)
    }

    @Test
    fun decodesLegacySixFieldRowsWithZeroResumePosition() {
        val legacyFields = listOf("local:7", "Legacy track", "Artist", "content://media/7", "audio/mpeg", "123")
        val legacyRow = legacyFields.joinToString("|") { value ->
            java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.toByteArray(Charsets.UTF_8))
        }

        val decoded = PlaybackHistoryCodec.decode(legacyRow)

        assertEquals(1, decoded.size)
        assertEquals(0L, decoded.single().resumePositionMs)
    }

    @Test
    fun roundTripsResumePositionAndClampsNegativePositions() {
        val entry = PlaybackHistoryEntry("local:9", "Track", null, "content://media/9", null, 123L, 90_000L)
        val decoded = PlaybackHistoryCodec.decode(PlaybackHistoryCodec.encode(listOf(entry)))

        assertEquals(90_000L, decoded.single().resumePositionMs)
        assertEquals(0L, PlaybackHistoryCodec.decode(PlaybackHistoryCodec.encode(listOf(entry.copy(resumePositionMs = -10L)))).single().resumePositionMs)
    }

    @Test
    fun deduplicatesByMediaIdentityAndCapsHistoryAtFiftyEntries() {
        val entries = (0..59).map { index ->
            PlaybackHistoryEntry("item-$index", "Item $index", null, "https://example.com/$index", null, index.toLong())
        }
        val duplicate = entries.first().copy(title = "Updated title", playedAtEpochMs = 100L)
        val decoded = PlaybackHistoryCodec.decode(PlaybackHistoryCodec.encode(listOf(duplicate) + entries))

        assertEquals(50, decoded.size)
        assertEquals("Updated title", decoded.first().title)
        assertTrue(decoded.none { it.mediaId == "item-59" })
    }
}
