package com.ysfclc.mosaic.core.library

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.ysfclc.mosaic.core.media.MediaSourceChange

/**
 * Lifecycle-bound observer for the device audio index.
 *
 * Only exact, supported item URIs become item-level signals. Collection, null,
 * malformed and otherwise ambiguous notifications remain invalidations.
 */
class MediaStoreChangeMonitor(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    /** Compatibility stream for callers that only understand invalidations. */
    fun observeAudioChanges(): Flow<Unit> = observeAudioChangeEvents().map { Unit }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAudioChangeEvents(): Flow<MediaSourceChange> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(MediaSourceChange.Invalidate)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(classify(uri))
            }
        }

        val registered = runCatching {
            resolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }.isSuccess

        awaitClose {
            if (registered) {
                runCatching { resolver.unregisterContentObserver(observer) }
            }
        }
    }.buffer(Channel.UNLIMITED).distinctUntilChanged { previous, current ->
        previous is MediaSourceChange.ItemChanged &&
            current is MediaSourceChange.ItemChanged &&
            previous == current
    }

    companion object {
        /** Pure URI classification so the item-level boundary can be regression-tested. */
        fun classify(uri: Uri?): MediaSourceChange {
            if (
                uri == null ||
                uri.scheme != "content" ||
                uri.authority != "media" ||
                uri.query != null ||
                uri.fragment != null
            ) {
                return MediaSourceChange.Invalidate
            }

            val segments = uri.pathSegments
            if (
                segments.size != 4 ||
                segments[0] !in setOf("external", "external_primary") ||
                segments[1] != "audio" ||
                segments[2] != "media"
            ) {
                return MediaSourceChange.Invalidate
            }

            val rowId = segments[3].toLongOrNull()?.takeIf { it > 0L }
                ?: return MediaSourceChange.Invalidate
            return MediaSourceChange.ItemChanged("local:$rowId")
        }
    }
}
