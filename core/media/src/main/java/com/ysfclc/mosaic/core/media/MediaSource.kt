package com.ysfclc.mosaic.core.media

import android.net.Uri

/** Stable, provider-neutral description of one item in the personal media library. */
data class MediaItemRecord(
    val id: String,
    val sourceId: String,
    val title: String,
    val creator: String? = null,
    val collection: String? = null,
    val durationMs: Long = 0L,
    val uri: String,
    val mimeType: String? = null,
    val artworkUri: String? = null,
    val kind: MediaKind = MediaKind.AUDIO,
    val addedAtEpochSeconds: Long? = null,
)

enum class MediaKind { AUDIO, VIDEO, PODCAST_EPISODE, RADIO_STREAM, WEB_CAPTURE, AUDIOBOOK }

enum class MediaCapability {
    BROWSE,
    SEARCH,
    NATIVE_PLAYBACK,
    BACKGROUND_PLAYBACK,
    SEEK,
    SPEED_CONTROL,
    OFFLINE_COPY,
    TRANSCRIPT,
    EXTERNAL_CONTROLS,
}

class SourceCapabilities(enabled: Set<MediaCapability>) {
    private val enabled = enabled.toSet()

    operator fun contains(capability: MediaCapability): Boolean = capability in enabled
    fun asSet(): Set<MediaCapability> = enabled.toSet()
    fun supportsNativeBackgroundAudio(): Boolean =
        MediaCapability.NATIVE_PLAYBACK in enabled && MediaCapability.BACKGROUND_PLAYBACK in enabled

    companion object {
        val LOCAL_MEDIA = SourceCapabilities(
            setOf(
                MediaCapability.BROWSE,
                MediaCapability.SEARCH,
                MediaCapability.NATIVE_PLAYBACK,
                MediaCapability.BACKGROUND_PLAYBACK,
                MediaCapability.SEEK,
                MediaCapability.SPEED_CONTROL,
                MediaCapability.EXTERNAL_CONTROLS,
            ),
        )
    }
}

data class MediaSourceDescriptor(
    val id: String,
    val name: String,
    val summary: String,
    val requiresNetwork: Boolean,
    val capabilities: SourceCapabilities,
)

data class ResolvedMediaSource(
    val uri: Uri,
    val mimeType: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
)

/** Every provider is an adapter. It never owns the canonical library or playback state. */
interface MediaSourceAdapter {
    val descriptor: MediaSourceDescriptor
    suspend fun browse(query: String = ""): List<MediaItemRecord>
    suspend fun resolve(mediaId: String): ResolvedMediaSource?
}
