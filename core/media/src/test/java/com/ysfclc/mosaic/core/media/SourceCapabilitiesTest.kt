package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCapabilitiesTest {
    @Test
    fun localMediaSupportsNativeBackgroundPlayback() {
        assertTrue(SourceCapabilities.LOCAL_MEDIA.supportsNativeBackgroundAudio())
    }

    @Test
    fun nativePlaybackWithoutBackgroundPermissionIsNotEquivalent() {
        val browserOnly = SourceCapabilities(setOf(MediaCapability.NATIVE_PLAYBACK))
        assertFalse(browserOnly.supportsNativeBackgroundAudio())
    }

    @Test
    fun capabilitiesAreDefensivelyCopiedAndNotExposedAsMutableInternalState() {
        val mutableInput = mutableSetOf(MediaCapability.NATIVE_PLAYBACK)
        val capabilities = SourceCapabilities(mutableInput)
        mutableInput += MediaCapability.BACKGROUND_PLAYBACK
        assertFalse(capabilities.supportsNativeBackgroundAudio())

        val copy = SourceCapabilities.LOCAL_MEDIA.asSet()
        assertTrue(MediaCapability.BACKGROUND_PLAYBACK in copy)
        assertTrue(MediaCapability.EXTERNAL_CONTROLS in copy)
    }
}
