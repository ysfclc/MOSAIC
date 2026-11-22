package com.ysfclc.mosaic

import android.net.Uri
import com.ysfclc.mosaic.core.library.MediaStoreChangeMonitor
import com.ysfclc.mosaic.core.media.MediaSourceChange
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreChangeClassificationTest {
    @Test
    fun supportedExternalAudioItemUrisBecomeTargetedChanges() {
        assertEquals(
            MediaSourceChange.ItemChanged("local:42"),
            MediaStoreChangeMonitor.classify(Uri.parse("content://media/external/audio/media/42")),
        )
        assertEquals(
            MediaSourceChange.ItemChanged("local:7"),
            MediaStoreChangeMonitor.classify(Uri.parse("content://media/external_primary/audio/media/7")),
        )
    }

    @Test
    fun collectionAndAmbiguousUrisRemainInvalidations() {
        val invalidation = MediaSourceChange.Invalidate
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(null))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("content://media/external/audio/media")))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("content://media/internal/audio/media/42")))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("content://media/external/audio/media/0")))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("content://media/external/audio/media/42?query=1")))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("content://media/external/audio/media/42#fragment")))
        assertEquals(invalidation, MediaStoreChangeMonitor.classify(Uri.parse("file:///external/audio/media/42")))
    }
}
