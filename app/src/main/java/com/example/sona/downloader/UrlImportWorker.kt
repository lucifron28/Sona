package com.example.sona.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.storage.AppMusicStorage
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlinx.coroutines.runBlocking

class UrlImportWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    private val processId = "sona-url-import-${id}"

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        DownloadLogger.info(downloadId.takeIf { it > 0L }, "Worker started workId=$id attempt=$runAttemptCount")
        if (downloadId <= 0L) {
            DownloadLogger.error(null, "Worker missing download id")
            return Result.failure()
        }

        val database = SonaDatabase.create(applicationContext)
        val downloadRepository = DownloadRepository(database.downloadDao())
        val songRepository = SongRepository(database.songDao())
        val appMusicStorage = AppMusicStorage(applicationContext)
        val downloadItem = downloadRepository.getDownload(downloadId)
        if (downloadItem == null) {
            DownloadLogger.error(downloadId, "No download row found for worker")
            database.close()
            return Result.failure()
        }

        return try {
            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.FETCHING_METADATA,
                diagnosticMessage = "Worker started. Initializing downloader.",
            )
            DownloadLogger.info(downloadId, "Initializing YoutubeDL and FFmpeg")
            YoutubeDL.getInstance().init(applicationContext)
            FFmpeg.getInstance().init(applicationContext)

            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.FETCHING_METADATA,
                diagnosticMessage = "Downloader initialized. Fetching metadata.",
            )
            DownloadLogger.info(downloadId, "Fetching metadata for ${downloadItem.url}")
            val videoInfo = YoutubeDL.getInstance().getInfo(singleVideoRequest(downloadItem.url))
            val title = videoInfo.title ?: videoInfo.fulltitle
            val artist = videoInfo.uploader
            DownloadLogger.info(downloadId, "Metadata fetched title=${title ?: "unknown"} artist=${artist ?: "unknown"}")

            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.DOWNLOADING,
                title = title,
                diagnosticMessage = "Metadata fetched. Preparing audio download.",
            )

            val outputDirectory = File(applicationContext.filesDir, "music").apply {
                mkdirs()
            }
            val outputTemplate = File(outputDirectory, "sona-import-$downloadId.%(ext)s")
            DownloadLogger.info(downloadId, "Output template=${outputTemplate.absolutePath}")
            val request = singleVideoRequest(downloadItem.url).apply {
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
                addOption("--audio-quality", "0")
                addOption("--newline")
                addOption("--no-mtime")
                addOption("-o", outputTemplate.absolutePath)
            }

            DownloadLogger.info(downloadId, "Starting yt-dlp execute processId=$processId")
            YoutubeDL.getInstance().execute(request, processId, true) { progress, _, _ ->
                val clampedProgress = progress.coerceIn(0f, 100f)
                DownloadLogger.debug(downloadId, "Progress ${clampedProgress.toInt()}%")
                runBlocking {
                    updateState(
                        repository = downloadRepository,
                        id = downloadId,
                        status = DownloadStatus.DOWNLOADING,
                        title = title,
                        progress = clampedProgress,
                        diagnosticMessage = "Downloading audio: ${clampedProgress.toInt()}%",
                    )
                }
            }
            DownloadLogger.info(downloadId, "yt-dlp execute finished")

            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.EXTRACTING,
                title = title,
                progress = 95f,
                diagnosticMessage = "Download finished. Locating extracted audio.",
            )

            val outputFile = findOutputFile(outputDirectory, downloadId)
                ?: error("Downloader finished but no audio output was found.")
            DownloadLogger.info(downloadId, "Output file found=${outputFile.absolutePath}")
            val song = appMusicStorage.registerDownloadedAudio(
                file = outputFile,
                fallbackTitle = title,
                fallbackArtist = artist,
                sourceUrl = downloadItem.url,
            )
            songRepository.addSong(song)
            DownloadLogger.info(downloadId, "Song added to library title=${song.title}")
            downloadRepository.markCompleted(
                id = downloadId,
                title = song.title,
                outputUri = song.uri,
            )

            Result.success()
        } catch (error: YoutubeDL.CanceledException) {
            DownloadLogger.error(downloadId, "Import cancelled", error)
            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.CANCELLED,
                diagnosticMessage = "Import cancelled.",
            )
            Result.failure()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            DownloadLogger.error(downloadId, "Import interrupted", error)
            downloadRepository.markFailed(downloadId, "Import interrupted.")
            Result.failure()
        } catch (error: Throwable) {
            DownloadLogger.error(downloadId, "Import failed", error)
            downloadRepository.markFailed(
                id = downloadId,
                errorMessage = error.message ?: "Import failed.",
            )
            Result.failure()
        } finally {
            database.close()
        }
    }

    private fun findOutputFile(outputDirectory: File, downloadId: Long): File? =
        outputDirectory
            .listFiles { file -> file.isFile && file.name.startsWith("sona-import-$downloadId.") }
            ?.maxByOrNull { it.lastModified() }

    private fun singleVideoRequest(url: String): YoutubeDLRequest =
        YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
        }

    private suspend fun updateState(
        repository: DownloadRepository,
        id: Long,
        status: DownloadStatus,
        title: String? = null,
        progress: Float = 0f,
        diagnosticMessage: String,
    ) {
        repository.updateState(
            id = id,
            status = status,
            title = title,
            progress = progress,
            diagnosticMessage = diagnosticMessage,
        )
    }
}
