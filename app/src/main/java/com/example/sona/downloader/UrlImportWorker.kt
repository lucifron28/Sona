package com.example.sona.downloader

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.storage.AppMusicStorage
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
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
        var lastDownloaderLine: String? = null
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
            return Result.failure()
        }

        return try {
            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.FETCHING_METADATA,
                diagnosticMessage = "Worker started. Initializing downloader.",
            )
            DownloadLogger.info(downloadId, "Initializing YoutubeDL")
            YoutubeDL.getInstance().init(applicationContext)

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
                diagnosticMessage = "Metadata fetched. Preparing best audio download.",
            )

            val outputDirectory = File(applicationContext.filesDir, "music").apply {
                mkdirs()
            }
            val outputTemplate = File(outputDirectory, "sona-import-$downloadId.%(ext)s")
            DownloadLogger.info(downloadId, "Output template=${outputTemplate.absolutePath}")
            var latestProgress = 0f
            var lastPersistedPercent = -1
            var downloadAttempt = 1
            var repairedDownloader = false
            while (true) {
                val request = audioDownloadRequest(downloadItem.url, outputTemplate)
                val attemptProcessId = "$processId-attempt-$downloadAttempt"
                DownloadLogger.info(
                    downloadId,
                    "Starting yt-dlp execute processId=$attemptProcessId attempt=$downloadAttempt",
                )
                try {
                    YoutubeDL.getInstance().execute(request, attemptProcessId, true) { progress, _, line ->
                        val downloaderLine = line.cleanedDownloaderLine()
                        if (downloaderLine != null) {
                            lastDownloaderLine = downloaderLine
                            DownloadLogger.debug(downloadId, "yt-dlp: $downloaderLine")
                        }

                        if (progress >= 0f) {
                            latestProgress = progress.coerceIn(0f, 100f)
                        }

                        val percent = latestProgress.toInt()
                        val shouldPersist = downloaderLine != null || percent != lastPersistedPercent
                        if (!shouldPersist) return@execute

                        lastPersistedPercent = percent
                        val diagnosticMessage = downloaderLine ?: "Downloading audio: $percent%"
                        runBlocking {
                            updateState(
                                repository = downloadRepository,
                                id = downloadId,
                                status = DownloadStatus.DOWNLOADING,
                                title = title,
                                progress = latestProgress,
                                diagnosticMessage = diagnosticMessage,
                            )
                        }
                    }
                    break
                } catch (error: YoutubeDLException) {
                    if (repairedDownloader || !error.shouldRepairYoutube403(downloadItem.url, lastDownloaderLine)) {
                        throw error
                    }

                    repairedDownloader = true
                    DownloadLogger.info(downloadId, "YouTube 403 detected; updating yt-dlp master before retry")
                    updateState(
                        repository = downloadRepository,
                        id = downloadId,
                        status = DownloadStatus.DOWNLOADING,
                        title = title,
                        progress = 0f,
                        diagnosticMessage = "YouTube returned 403. Updating downloader and retrying once.",
                    )
                    val updateMessage = DownloaderUpdater.update(
                        context = applicationContext,
                        channel = YoutubeDL.UpdateChannel.MASTER,
                        reason = "YouTube 403 repair",
                    )
                    DownloadLogger.info(downloadId, "yt-dlp master update result=$updateMessage")

                    lastDownloaderLine = null
                    latestProgress = 0f
                    lastPersistedPercent = -1
                    downloadAttempt += 1
                    updateState(
                        repository = downloadRepository,
                        id = downloadId,
                        status = DownloadStatus.DOWNLOADING,
                        title = title,
                        progress = 0f,
                        diagnosticMessage = "$updateMessage. Retrying audio download.",
                    )
                }
            }
            DownloadLogger.info(downloadId, "yt-dlp execute finished")

            DownloadLogger.info(downloadId, "Finalizing downloaded audio")
            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.EXTRACTING,
                title = title,
                progress = 99f,
                diagnosticMessage = "Audio downloaded. Adding to library.",
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
            database.withTransaction {
                songRepository.addSong(song)
                downloadRepository.markCompleted(
                    id = downloadId,
                    title = song.title,
                    outputUri = song.uri,
                )
            }
            DownloadLogger.info(downloadId, "Song added and download completed title=${song.title}")

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
            val errorMessage = error.userVisibleDownloadMessage(lastDownloaderLine)
            DownloadLogger.error(downloadId, "Import failed: $errorMessage", error)
            downloadRepository.markFailed(
                id = downloadId,
                errorMessage = errorMessage,
            )
            Result.failure()
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

    private fun audioDownloadRequest(
        url: String,
        outputTemplate: File,
    ): YoutubeDLRequest =
        singleVideoRequest(url).apply {
            addOption("-f", AUDIO_FORMAT_SELECTOR)
            addOption("--newline")
            addOption("--no-mtime")
            addOption("-o", outputTemplate.absolutePath)
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

    private companion object {
        const val AUDIO_FORMAT_SELECTOR = "bestaudio[ext=m4a]/bestaudio/best"
    }
}
