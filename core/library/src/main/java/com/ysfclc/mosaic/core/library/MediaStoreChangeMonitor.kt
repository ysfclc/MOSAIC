package com.ysfclc.mosaic.core.library

import android.content.ContentObserver
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Lifecycle-bound invalidation signal for the device audio index.
 *
 * MediaStore owns the actual records; this observer only signals that a fresh
 * provider snapshot may be needed. It never performs database work in a callback.
 */
class MediaStoreChangeMonitor(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAudioChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }

            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                trySend(Unit)
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
    }.conflate()
}
