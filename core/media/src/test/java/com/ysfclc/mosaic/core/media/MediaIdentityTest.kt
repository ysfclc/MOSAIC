package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaIdentityTest {
    @Test
    fun scopesIdenticalSourceItemIdsToTheirSource() {
        val local = MediaIdentity.stableId("device-library", "local:42")
        val podcast = MediaIdentity.stableId("podcast-feed", "local:42")

        assertNotEquals(local, podcast)
        assertEquals("local:42", MediaIdentity.sourceItemId(local, "device-library"))
        assertNull(MediaIdentity.sourceItemId(local, "podcast-feed"))
    }

    @Test
    fun roundTripsUnicodeAndDelimiterCharacters() {
        val stableId = MediaIdentity.stableId("音楽:source", "episode:ä:🎧")
        val parsed = MediaIdentity.parse(stableId)

        assertNotNull(parsed)
        assertEquals("音楽:source", parsed!!.sourceId)
        assertEquals("episode:ä:🎧", parsed.sourceItemId)
    }

    @Test
    fun recordIdentityUsesSourceNamespaceAndFallsBackToRecordUriForBlankId() {
        val record = MediaItemRecord(
            id = "",
            sourceId = "device-library",
            title = "A track",
            uri = "content://media/external/audio/media/42",
        )

        assertEquals(
            MediaIdentity.stableId("device-library", record.uri),
            MediaIdentity.forRecord(record),
        )
    }

    @Test
    fun refusesBlankSourceOrItemIdsInsteadOfCreatingCollisions() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MediaIdentity.stableId("", "item")
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MediaIdentity.stableId("source", "")
        }
    }

    @Test
    fun rejectsMalformedOrForeignIdentityStrings() {
        assertNull(MediaIdentity.parse("local:42"))
        assertNull(MediaIdentity.parse("mosaic-media-v1:%%%:bad"))
        assertNull(MediaIdentity.parse("other-prefix:abc:def"))
    }
}
