package com.ysfclc.mosaic.core.library

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.ysfclc.mosaic.core.media.MediaBrowseResult
import com.ysfclc.mosaic.core.media.MediaIdentity
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.MediaKind
import com.ysfclc.mosaic.core.media.MediaSourceAdapter
import com.ysfclc.mosaic.core.media.MediaSourceChange
import com.ysfclc.mosaic.core.media.MediaSourceDescriptor
import com.ysfclc.mosaic.core.media.ResolvedMediaSource
import com.ysfclc.mosaic.core.media.SourceCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow

/** Read-only MediaStore index. Original files are never copied, moved, or renamed. */
class MediaStoreCatalog(private val context: Context) {
    private val changeMonitor = MediaStoreChangeMonitor(context)

    /** Emits invalidations while collected; unregisters when collection stops. */
    fun observeAudioChanges(): Flow<Unit> = changeMonitor.observeAudioChanges()

    /** Item-specific signals are emitted only for recognized MediaStore audio-item URIs. */
    fun observeAudioChangeEvents(): Flow<MediaSourceChange> = changeMonitor.observeAudioChangeEvents()

    suspend fun scanAudio(query: String = ""): List<MediaItemRecord> = scanAudioSnapshot(query).items

    suspend fun scanAudioSnapshot(query: String = ""): MediaBrowseResult {
        val selection = if (query.isBlank()) null else "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val selectionArgs = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        return queryAudioRecords(selection, selectionArgs, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")
    }

    /**
     * Reads one row by MediaStore ID without enumerating the entire library.
     * Null is deliberately ambiguous (missing, inaccessible or provider failure);
     * callers must not treat it as deletion evidence.
     */
    suspend fun findAudioById(rowId: Long): MediaItemRecord? {
        if (rowId <= 0L) return null
        val result = queryAudioRecords("${MediaStore.Audio.Media._ID} = ?", arrayOf(rowId.toString()), null)
        return result.items.firstOrNull()
    }

    private suspend fun queryAudioRecords(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): MediaBrowseResult = withContext(Dispatchers.IO) {
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
        val results = mutableListOf<MediaItemRecord>()
        var completeSnapshot = false

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )
            if (cursor != null) {
                cursor.use { rows ->
                    while (rows.moveToNext()) results += rows.toAudioRecord()
                }
                completeSnapshot = selection == null
            }
        } catch (_: SecurityException) {
            // Permission failure or mid-query revocation is an incomplete result.
        }
        MediaBrowseResult(results, isCompleteSnapshot = completeSnapshot)
    }

    private fun Cursor.toAudioRecord(): MediaItemRecord {
        val rowId = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val title = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        val artistColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val mimeColumn = getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val dateColumn = getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val albumIdColumn = getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val albumId = albumIdColumn.takeIf { it >= 0 }?.let { getLong(it) } ?: -1L
        val artwork = if (albumId >= 0L) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
        } else null

        return MediaItemRecord(
            id = "local:$rowId",
            sourceId = "device-library",
            title = title?.takeIf(String::isNotBlank) ?: "Untitled audio",
            creator = getStringOrNull(artistColumn)?.takeUnless { it == "<unknown>" },
            collection = getStringOrNull(albumColumn)?.takeUnless { it == "<unknown>" },
            durationMs = getLong(durationColumn).coerceAtLeast(0L),
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rowId).toString(),
            mimeType = if (mimeColumn >= 0) getStringOrNull(mimeColumn) else null,
            artworkUri = artwork,
            kind = MediaKind.AUDIO,
            addedAtEpochSeconds = if (dateColumn >= 0) getLong(dateColumn) else null,
        )
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

    override suspend fun browseSnapshot(query: String): MediaBrowseResult = catalog.scanAudioSnapshot(query)

    override fun observeChanges(): Flow<Unit> = catalog.observeAudioChanges()

    override fun observeChangeEvents(): Flow<MediaSourceChange> = catalog.observeAudioChangeEvents()

    override suspend fun findItem(sourceItemId: String): MediaItemRecord? {
        if (!sourceItemId.startsWith("local:")) return null
        val rowId = sourceItemId.removePrefix("local:").toLongOrNull() ?: return null
        return catalog.findAudioById(rowId)
    }

    override suspend fun resolve(mediaId: String): ResolvedMediaSource? {
        val sourceItemId = MediaIdentity.sourceItemId(mediaId, descriptor.id) ?: mediaId
        val record = findItem(sourceItemId) ?: return null
        return ResolvedMediaSource(Uri.parse(record.uri), record.mimeType)
    }
}
