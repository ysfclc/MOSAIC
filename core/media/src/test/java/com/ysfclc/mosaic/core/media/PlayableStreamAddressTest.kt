package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayableStreamAddressTest {
    @Test
    fun acceptsHttpsAudioAndRadioAddresses() {
        assertEquals(
            "https://radio.example/live.mp3",
            PlayableStreamAddress.normalize(" https://radio.example/live.mp3 "),
        )
        assertEquals(
            "https://cdn.example/live/index.m3u8",
            PlayableStreamAddress.normalize("https://cdn.example/live/index.m3u8"),
        )
    }

    @Test
    fun rejectsCleartextAndNonNetworkSchemes() {
        assertNull(PlayableStreamAddress.normalize("http://radio.example/live.mp3"))
        assertNull(PlayableStreamAddress.normalize("file:///sdcard/music.mp3"))
        assertNull(PlayableStreamAddress.normalize("javascript:alert(1)"))
    }

    @Test
    fun rejectsMissingHostsAndEmbeddedCredentials() {
        assertNull(PlayableStreamAddress.normalize("https:///live.mp3"))
        assertNull(PlayableStreamAddress.normalize("https://user:pass@radio.example/live.mp3"))
        assertNull(PlayableStreamAddress.normalize("not a url"))
    }
}
