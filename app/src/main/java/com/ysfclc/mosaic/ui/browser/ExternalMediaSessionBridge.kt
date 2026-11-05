package com.ysfclc.mosaic.ui.browser

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MosaicMediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Intentionally do not read notification text or content.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // The UI queries active MediaSessions, not notification contents.
    }
}

/**
 * A source-owned Android media session. Commands are delegated to the original app;
 * this object never represents audio that MOSAIC has acquired or is playing itself.
 */
data class ExternalMediaSession(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val canPlayPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
    val canSeek: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    private val controller: MediaController,
) {
    fun togglePlayback() {
        if (isPlaying) controller.transportControls.pause()
        else controller.transportControls.play()
    }

    fun skipNext() {
        if (canSkipNext) controller.transportControls.skipToNext()
    }

    fun skipPrevious() {
        if (canSkipPrevious) controller.transportControls.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        if (canSeek) {
            val target = if (durationMs > 0L) positionMs.coerceIn(0L, durationMs) else positionMs.coerceAtLeast(0L)
            controller.transportControls.seekTo(target)
        }
    }
}

object ExternalMediaSessionBridge {
    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val expected = ComponentName(context, MosaicMediaNotificationListener::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun activeSessions(context: Context): List<ExternalMediaSession> {
        if (!hasNotificationAccess(context)) return emptyList()
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listener = ComponentName(context, MosaicMediaNotificationListener::class.java)
        return runCatching { manager.getActiveSessions(listener) }
            .getOrDefault(emptyList())
            .asSequence()
            .filter { it.packageName != context.packageName }
            .map { controller ->
                val metadata = controller.metadata
                val state = controller.playbackState
                val actions = state?.actions ?: 0L
                val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                val appLabel = runCatching {
                    val info = context.packageManager.getApplicationInfo(controller.packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                }.getOrDefault(controller.packageName)
                ExternalMediaSession(
                    packageName = controller.packageName,
                    appLabel = appLabel,
                    title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty().ifBlank { "Unknown media" },
                    artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                    canPlayPause = actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE) != 0L,
                    canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
                    canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
                    canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
                    positionMs = state?.position?.coerceAtLeast(0L) ?: 0L,
                    durationMs = duration.coerceAtLeast(0L),
                    controller = controller,
                )
            }
            .toList()
    }
}
