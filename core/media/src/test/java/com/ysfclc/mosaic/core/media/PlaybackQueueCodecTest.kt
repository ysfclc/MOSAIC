package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueCodecTest {
    @Test
    fun roundTripsQueueMetadataCurrentItemAndPosition() {
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(
                PersistedQueueItem("episode:1", "Zażółć 🎧", "Artist", "https://example.com/one.mp3", "audio/mpeg"),
                PersistedQueueItem("local:2", "Local file", null, "content://media/external/audio/media/2", null),
            ),
            currentIndex = 1,
            positionMs = 245_000L,
        )

        assertEquals(snapshot, PlaybackQueueCodec.decode(PlaybackQueueCodec.encode(snapshot)))
    }

    @Test
    fun omitsSignedAndCredentialBearingUrlsAndRemapsCurrentIndex() {
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(
                PersistedQueueItem("one", "One", null, "https://example.com/one.mp3", null),
                PersistedQueueItem("signed", "Signed", null, "https://example.com/audio.mp3?token=secret", null),
                PersistedQueueItem("three", "Three", null, "https://example.com/three.mp3", null),
                PersistedQueueItem("credentials", "Credentials", null, "https://user:pass@example.com/four.mp3", null),
            ),
            currentIndex = 2,
            positionMs = 9000L,
        )

        val decoded = PlaybackQueueCodec.decode(PlaybackQueueCodec.encode(snapshot))

        assertNotNull(decoded)
        assertEquals(listOf("one", "three"), decoded!!.items.map { it.mediaId })
        assertEquals(1, decoded.currentIndex)
        assertEquals(9000L, decoded.positionMs)
    }

    @Test
    fun movesForwardWhenSelectedQueueItemCannotBePersisted() {
        val snapshot = PlaybackQueueSnapshot(
            items = listOf(
                PersistedQueueItem("one", "One", null, "https://example.com/one.mp3", null),
                PersistedQueueItem("signed", "Signed", null, "https://example.com/signed.mp3?token=private", null),
                PersistedQueueItem("three", "Three", null, "https://example.com/three.mp3", null),
            ),
            currentIndex = 1,
            positionMs = 5000L,
        )

        val decoded = PlaybackQueueCodec.decode(PlaybackQueueCodec.encode(snapshot))

        assertNotNull(decoded)
        assertEquals("three", decoded!!.items[decoded.currentIndex].mediaId)
        assertEquals(0L, decoded.positionMs)
    }

    @Test
    fun toleratesMalformedRowsWithoutShiftingTheSelectedItem() {
        val valid = PlaybackQueueCodec.encode(
            PlaybackQueueSnapshot(
                items = listOf(
                    PersistedQueueItem("one", "One", null, "https://example.com/one.mp3", null),
                    PersistedQueueItem("two", "Two", null, "https://example.com/two.mp3", null),
                ),
                currentIndex = 1,
                positionMs = 1200L,
            ),
        )
        val lines = valid.lines()
        val decoded = PlaybackQueueCodec.decode(listOf(lines.first(), lines[1], lines[2], "broken-row").joinToString("\n"))

        assertNotNull(decoded)
        assertEquals(listOf("one", "two"), decoded!!.items.map { it.mediaId })
        assertEquals(1, decoded.currentIndex)
    }

    @Test
    fun keepsCurrentItemInsideTheHundredItemBound() {
        val items = (0..119).map { index ->
            PersistedQueueItem("track-$index", "Track $index", null, "https://example.com/$index.mp3", "audio/mpeg")
        }
        val decoded = PlaybackQueueCodec.decode(PlaybackQueueCodec.encode(PlaybackQueueSnapshot(items, 110, 33_000L)))

        assertNotNull(decoded)
        assertEquals(100, decoded!!.items.size)
        assertEquals("track-110", decoded.items[decoded.currentIndex].mediaId)
        assertEquals(33_000L, decoded.positionMs)
    }

    @Test
    fun rejectsUnsupportedAndMalformedReferencesAndClampsNegativePosition() {
        val unsafe = PlaybackQueueSnapshot(
            items = listOf(
                PersistedQueueItem("javascript", "Bad", null, "javascript:alert(1)", null),
                PersistedQueueItem("fragment", "Fragment", null, "https://example.com/a.mp3#part", null),
                PersistedQueueItem("local", "Local", null, "content://media/external/audio/media/7", null),
            ),
            currentIndex = 2,
            positionMs = -5L,
        )

        val decoded = PlaybackQueueCodec.decode(PlaybackQueueCodec.encode(unsafe))

        assertNotNull(decoded)
        assertEquals(listOf("local"), decoded!!.items.map { it.mediaId })
        assertEquals(0, decoded.currentIndex)
        assertEquals(0L, decoded.positionMs)
        assertNull(PlaybackQueueCodec.decode("not-a-queue"))
        assertTrue(PlaybackQueueCodec.encode(PlaybackQueueSnapshot(emptyList(), 0, 0L)).isEmpty())
    }
}
