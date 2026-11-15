package com.ysfclc.mosaic.core.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun MediaItemRecord.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(creator)
        .setAlbumTitle(collection)
        .apply { artworkUri?.let { setArtworkUri(Uri.parse(it)) } }
        .build()

    return MediaItem.Builder()
        .setMediaId(MediaIdentity.forRecord(this))
        .setUri(Uri.parse(uri))
        .apply { mimeType?.let { setMimeType(it) } }
        .setMediaMetadata(metadata)
        .build()
}
