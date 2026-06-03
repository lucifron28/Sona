package com.example.sona.storage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.sona.domain.model.Song
import com.example.sona.domain.model.SourceType
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppMusicStorage(
    private val context: Context,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun importAudio(sourceUri: Uri): Song = withContext(Dispatchers.IO) {
        val displayName = sourceUri.displayName()
        val storedUri = copyIntoMusicStorage(sourceUri, displayName)
        val metadata = readAudioMetadata(storedUri)

        Song(
            title = metadata.title ?: displayName?.titleWithoutExtension() ?: "Imported audio",
            artist = metadata.artist ?: "Unknown artist",
            album = metadata.album,
            durationMs = metadata.durationMs,
            uri = storedUri.toString(),
            dateAdded = now(),
            sourceType = SourceType.LOCAL_FILE,
        )
    }

    private fun copyIntoMusicStorage(sourceUri: Uri, displayName: String?): Uri {
        val musicDirectory = File(context.filesDir, "music").apply {
            mkdirs()
        }
        val extension = displayName?.fileExtension()
            ?: sourceUri.mimeExtension()
            ?: "audio"
        val targetFile = File(musicDirectory, "${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open selected audio file." }
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return Uri.fromFile(targetFile)
    }

    private fun readAudioMetadata(uri: Uri): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            AudioMetadata(
                title = retriever.extractString(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractString(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractString(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever
                    .extractString(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L,
            )
        } catch (_: RuntimeException) {
            AudioMetadata()
        } finally {
            retriever.release()
        }
    }

    private fun Uri.displayName(): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(this, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else {
                null
            }
        }
    }

    private fun Uri.mimeExtension(): String? {
        val mimeType = context.contentResolver.getType(this) ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    private fun MediaMetadataRetriever.extractString(keyCode: Int): String? =
        extractMetadata(keyCode)?.takeIf { it.isNotBlank() }

    private fun String.fileExtension(): String? =
        substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }

    private fun String.titleWithoutExtension(): String =
        substringBeforeLast('.', missingDelimiterValue = this)
}

private data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
)
