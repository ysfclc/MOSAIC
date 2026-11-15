package com.ysfclc.mosaic.core.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.media.MediaSourceDescriptor
import com.ysfclc.mosaic.core.media.ResolvedMediaSource
import com.ysfclc.mosaic.core.media.SourceCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only MediaStore index. Original files are never copied, moved, or renamed. */
class MediaStoreCatalog(private val context: Context) {
    suspend fun scanAudio(query: String = ""): List<MediaItemRecord> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
        )
        val selection = if (query.isBlank()) null else "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val selectionArgs = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        val results = mutableListOf<MediaItemRecord>()

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val rowId = cursor.getLong(idColumn)
                    val albumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else -1L
                    val artwork = if (albumId >= 0L) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null
                    results += MediaItemRecord(
                        id = "local:$rowId",
                        sourceId = "device-library",
                        title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: "Untitled audio",
                        creator = cursor.getStringOrNull(artistColumn)?.takeUnless { it == "<unknown>" },
                        collection = cursor.getStringOrNull(albumColumn)?.takeUnless { it == "<unknown>" },
                        durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rowId).toString(),
                        mimeType = if (mimeColumn >= 0) cursor.getStringOrNull(mimeColumn) else null,
                        artworkUri = artwork,
                        kind = MediaKind.AUDIO,
                        addedAtEpochSeconds = if (dateColumn >= 0) cursor.getLong(dateColumn) else null,
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission can be revoked between the UI check and the query. Return an empty view safely.
        }
        results
    }
}

/** Phase-one implementation of the source contract for Android's existing media index. */
class LocalMediaSourceAdapter(private val catalog: MediaStoreCatalog) : MediaSourceAdapter {
    override val descriptor = MediaSourceDescriptor(
        id = "device-library",
        name = "Device library",
        summary = "Read-only index of audio already stored on this device",
        requiresNetwork = false,
        capabilities = SourceCapabilities.LOCAL_MEDIA,
    )

    override suspend fun browse(query: String): List<MediaItemRecord> = catalog.scanAudio(query)

    override suspend fun resolve(mediaId: String): ResolvedMediaSource? {
        val sourceItemId = MediaIdentity.sourceItemId(mediaId, descriptor.id) ?: mediaId
        val record = catalog.scanAudio().firstOrNull { it.id == sourceItemId } ?: return null
        return ResolvedMediaSource(Uri.parse(record.uri), record.mimeType)
    }
}
