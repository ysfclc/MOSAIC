package com.ysfclc.mosaic.core.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Activity-scoped controller connection. The service, not this class, owns the player. */
class PlaybackConnection(context: Context) {
    private val appContext = context.applicationContext
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingCommand: ((MediaController) -> Unit)? = null

    fun connect() {
        if (controllerFuture != null || _controller.value != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val connected = future.get()
                    if (controllerFuture === future) {
                        _controller.value = connected
                        _connectionError.value = null
                        pendingCommand?.also { command ->
                            pendingCommand = null
                            command(connected)
                        }
                    } else {
                        connected.release()
                    }
                } catch (exception: Exception) {
                    if (controllerFuture === future) {
                        controllerFuture = null
                        _connectionError.value = exception.cause?.message ?: exception.message ?: "Playback service unavailable"
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun play(records: List<MediaItemRecord>, startIndex: Int = 0) {
        if (records.isEmpty()) return
        val snapshot = records.toList()
        val safeIndex = startIndex.coerceIn(snapshot.indices)
        dispatch { activeController ->
            activeController.setMediaItems(snapshot.map(MediaItemRecord::toMedia3Item), safeIndex, 0L)
            activeController.prepare()
            activeController.play()
        }
    }

    fun playUri(uri: Uri, title: String, mimeType: String? = null) {
        dispatch { activeController ->
            val metadata = MediaMetadata.Builder().setTitle(title).build()
            val item = MediaItem.Builder()
                .setMediaId(uri.toString())
                .setUri(uri)
                .apply { mimeType?.let { setMimeType(it) } }
                .setMediaMetadata(metadata)
                .build()
            activeController.setMediaItem(item)
            activeController.prepare()
            activeController.play()
        }
    }

    private fun dispatch(command: (MediaController) -> Unit) {
        val activeController = _controller.value
        if (activeController != null) {
            command(activeController)
        } else {
            pendingCommand = command
            connect()
        }
    }

    fun release() {
        val future = controllerFuture
        controllerFuture = null
        pendingCommand = null
        _controller.value = null
        if (future != null) MediaController.releaseFuture(future)
    }
}
