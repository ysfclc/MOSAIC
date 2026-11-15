package com.ysfclc.mosaic.core.media

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns the sole app-wide player so audio survives Activity and UI lifecycle changes. */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var queueStore: PlaybackQueueStore? = null
    private val checkpointHandler = Handler(Looper.getMainLooper())
    private val queueListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            persistQueue(player)
        }
    }
    private val checkpointRunnable = object : Runnable {
        override fun run() {
            mediaSession?.player?.let(::persistQueue)
            checkpointHandler.postDelayed(this, CHECKPOINT_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        queueStore = PlaybackQueueStore(applicationContext)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setId("mosaic-primary-session")
            .build()

        player.addListener(queueListener)
        restoreQueue(player)
        checkpointHandler.postDelayed(checkpointRunnable, CHECKPOINT_INTERVAL_MS)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        checkpointHandler.removeCallbacks(checkpointRunnable)
        mediaSession?.let { session ->
            persistQueue(session.player)
            session.player.removeListener(queueListener)
            session.player.release()
            session.release()
        }
        mediaSession = null
        queueStore = null
        super.onDestroy()
    }

    private fun restoreQueue(player: Player) {
        val snapshot = queueStore?.load() ?: return
        if (snapshot.items.isEmpty()) return
        val restoredItems = snapshot.items.map { saved ->
            val metadata = MediaMetadata.Builder()
                .setTitle(saved.title)
                .apply { saved.creator?.let(::setArtist) }
                .build()
            MediaItem.Builder()
                .setMediaId(saved.mediaId)
                .setUri(Uri.parse(saved.uri))
                .apply { saved.mimeType?.let(::setMimeType) }
                .setMediaMetadata(metadata)
                .build()
        }
        player.setMediaItems(restoredItems, snapshot.currentIndex, snapshot.positionMs)
        player.prepare()
        // Restoring a queue must never unexpectedly restart audio after process death.
        player.pause()
    }

    private fun persistQueue(player: Player) {
        val store = queueStore ?: return
        if (player.mediaItemCount == 0) {
            store.clear()
            return
        }

        val indexedItems = (0 until player.mediaItemCount).mapNotNull { index ->
            val mediaItem = player.getMediaItemAt(index)
            val configuration = mediaItem.localConfiguration ?: return@mapNotNull null
            val uri = configuration.uri
            val metadata = mediaItem.mediaMetadata
            index to PersistedQueueItem(
                mediaId = mediaItem.mediaId.ifBlank { uri.toString() },
                title = metadata.title?.toString()?.takeIf(String::isNotBlank)
                    ?: uri.lastPathSegment?.takeIf(String::isNotBlank)
                    ?: "Untitled media",
                creator = metadata.artist?.toString()?.takeIf(String::isNotBlank),
                uri = uri.toString(),
                mimeType = configuration.mimeType,
            )
        }
        if (indexedItems.isEmpty()) {
            store.clear()
            return
        }

        val selectedIndex = indexedItems.indexOfFirst { it.first == player.currentMediaItemIndex }
            .takeIf { it >= 0 } ?: 0
        store.save(
            PlaybackQueueSnapshot(
                items = indexedItems.map { it.second },
                currentIndex = selectedIndex,
                positionMs = player.currentPosition.coerceAtLeast(0L),
            ),
        )
    }

    private companion object {
        const val CHECKPOINT_INTERVAL_MS = 10_000L
    }
}
