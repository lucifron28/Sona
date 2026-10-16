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
import java.security.MessageDigest
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
            artworkUri = metadata.artworkUri,
            dateAdded = now(),
            sourceType = SourceType.LOCAL_FILE,
        )
    }

    suspend fun registerDownloadedAudio(
        file: File,
        fallbackTitle: String?,
        fallbackArtist: String?,
        sourceUrl: String,
    ): Song = withContext(Dispatchers.IO) {
        val storedUri = Uri.fromFile(file)
        val metadata = readAudioMetadata(storedUri)

        Song(
            title = metadata.title ?: fallbackTitle ?: file.name.titleWithoutExtension(),
            artist = metadata.artist ?: fallbackArtist ?: "Unknown artist",
            album = metadata.album,
            durationMs = metadata.durationMs,
            uri = storedUri.toString(),
            artworkUri = metadata.artworkUri,
            dateAdded = now(),
            sourceType = SourceType.IMPORTED_URL,
            sourceUrl = sourceUrl,
        )
    }

    internal suspend fun isDownloadedAudioAvailable(uriString: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val path = uri.path ?: return@withContext false
                val file = File(path)
                uri.scheme == "file" &&
                    file.isAvailableStoredAudioIn(File(context.filesDir, "music"))
            }.getOrDefault(false)
        }

    suspend fun deleteStoredAudio(song: Song) = withContext(Dispatchers.IO) {
        val file = runCatching {
            File(requireNotNull(Uri.parse(song.uri).path))
        }.getOrNull() ?: return@withContext

        if (file.isStoredInMusicDirectory()) {
            file.delete()
        }
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

    private fun File.isStoredInMusicDirectory(): Boolean =
        isWithinDirectory(File(context.filesDir, "music"))

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
                artworkUri = runCatching {
                    retriever.embeddedPicture?.let(::storeEmbeddedArtwork)
                }.getOrNull(),
            )
        } catch (_: RuntimeException) {
            AudioMetadata()
        } finally {
            retriever.release()
        }
    }

    private fun storeEmbeddedArtwork(image: ByteArray): String? {
        if (image.isEmpty()) return null
        val artworkDirectory = File(context.filesDir, "artwork")
        if (!artworkDirectory.exists() && !artworkDirectory.mkdirs()) return null

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(image)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
        val artworkFile = File(artworkDirectory, "$hash.img")
        if (!artworkFile.exists()) {
            val temporaryFile = File(artworkDirectory, "${UUID.randomUUID()}.tmp")
            try {
                temporaryFile.outputStream().use { output -> output.write(image) }
                if (!temporaryFile.renameTo(artworkFile) && !artworkFile.exists()) return null
            } catch (_: Exception) {
                return null
            } finally {
                temporaryFile.delete()
            }
        }
        return Uri.fromFile(artworkFile).toString()
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

internal fun File.isAvailableStoredAudioIn(musicDirectory: File): Boolean =
    isFile && length() > 0L && isWithinDirectory(musicDirectory)

private fun File.isWithinDirectory(directory: File): Boolean = runCatching {
    val canonicalDirectory = directory.canonicalFile
    val canonicalFile = canonicalFile
    canonicalFile.path.startsWith(canonicalDirectory.path + File.separator)
}.getOrDefault(false)

private data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
)
