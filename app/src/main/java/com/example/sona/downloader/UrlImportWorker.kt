package com.example.sona.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.storage.AppMusicStorage
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class UrlImportWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val downloader: UrlDownloadExecutor,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        DownloadLogger.info(downloadId.takeIf { it > 0L }, "Worker started workId=$id attempt=$runAttemptCount")
        if (downloadId <= 0L) {
            DownloadLogger.error(null, "Worker missing download id")
            return Result.success()
        }

        setForeground(foregroundInfo(downloadId, "Preparing download", progress = null))

        val database = SonaDatabase.create(applicationContext)
        val downloadRepository = DownloadRepository(database.downloadDao())
        val songRepository = SongRepository(database.songDao())
        val appMusicStorage = AppMusicStorage(applicationContext)
        val downloadItem = downloadRepository.getDownload(downloadId)
        if (downloadItem == null) {
            DownloadLogger.error(downloadId, "No download row found for worker")
            return Result.success()
        }
        if (downloadItem.workId != id.toString()) {
            DownloadLogger.info(downloadId, "Skipping stale worker workId=$id")
            return Result.success()
        }
        if (downloadItem.status.isTerminal) return Result.success()

        val queueDelayMs = (System.currentTimeMillis() - downloadItem.createdAt).coerceAtLeast(0L)
        DownloadLogger.info(downloadId, "timing queue_delay_ms=$queueDelayMs")
        val workerStartedAt = SystemClock.elapsedRealtime()
        val lastDownloaderLine = AtomicReference<String?>(null)

        return try {
            val outputDirectory = File(applicationContext.filesDir, "music").apply {
                if (!exists() && !mkdirs()) error("Unable to create audio storage directory.")
            }
            val outputTemplate = File(outputDirectory, "sona-import-$downloadId.%(ext)s")

            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.FETCHING_METADATA,
                title = downloadItem.title,
                progress = downloadItem.progress,
                diagnosticMessage = "Preparing audio import.",
            )
            val latestProgress = AtomicReference(downloadItem.progress)
            val downloadingStarted = AtomicBoolean(false)
            val progressEvents = Channel<DownloaderProgress>(Channel.CONFLATED)

            coroutineScope {
                val progressJob = launch {
                    var lastPersistedAt = 0L
                    for (update in progressEvents) {
                        if (update.percent >= 0f) {
                            val progress = update.percent.coerceIn(0f, 100f)
                            if (progress > latestProgress.get()) latestProgress.set(progress)
                            if (downloadingStarted.compareAndSet(false, true)) {
                                updateState(
                                    repository = downloadRepository,
                                    id = downloadId,
                                    status = DownloadStatus.DOWNLOADING,
                                    title = downloadItem.title,
                                    progress = latestProgress.get(),
                                    diagnosticMessage = "Downloading audio.",
                                )
                            }
                        }
                        update.line?.let(lastDownloaderLine::set)

                        val now = SystemClock.elapsedRealtime()
                        val waitMs = (PROGRESS_PERSIST_INTERVAL_MS - (now - lastPersistedAt))
                            .coerceAtLeast(0L)
                        if (waitMs > 0L) delay(waitMs)

                        val progress = latestProgress.get().coerceIn(0f, 100f)
                        downloadRepository.updateProgress(
                            id = downloadId,
                            progress = progress,
                            title = downloadItem.title,
                            diagnosticMessage = lastDownloaderLine.get(),
                        )
                        val stage = if (downloadingStarted.get()) {
                            "Downloading audio"
                        } else {
                            "Preparing audio stream"
                        }
                        setForeground(
                            foregroundInfo(
                                downloadId,
                                stage,
                                progress.takeIf { downloadingStarted.get() },
                            ),
                        )
                        lastPersistedAt = SystemClock.elapsedRealtime()
                    }
                }
                val operationCompletedNormally = AtomicBoolean(false)
                val cancellationMonitor = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        if (!operationCompletedNormally.get()) {
                            DownloaderProcessRegistry.cancel(downloadId)
                        }
                    }
                }

                try {
                    var downloadAttempt = 1
                    var repairedDownloader = false
                    while (true) {
                        val attemptStartedAt = SystemClock.elapsedRealtime()
                        val transferStartedAt = AtomicLong(0L)
                        val initializationFinishedAt = AtomicLong(0L)
                        val attemptProcessId = "sona-url-import-$id-attempt-$downloadAttempt"
                        DownloaderProcessRegistry.register(downloadId, attemptProcessId, downloader)
                        try {
                            val executeFinishedAt = DownloaderOperationLock.mutex.withLock {
                                val operationStartedAt = SystemClock.elapsedRealtime()
                                val operationWaitMs = operationStartedAt - attemptStartedAt
                                DownloadLogger.info(
                                    downloadId,
                                    "timing downloader_operation_wait_ms=$operationWaitMs",
                                )

                                val initializationStartedAt = SystemClock.elapsedRealtime()
                                downloader.initialize()
                                val initializedAt = SystemClock.elapsedRealtime()
                                initializationFinishedAt.set(initializedAt)
                                DownloadLogger.info(
                                    downloadId,
                                    "timing downloader_initialization_ms=${initializedAt - initializationStartedAt}",
                                )
                                updateState(
                                    repository = downloadRepository,
                                    id = downloadId,
                                    status = DownloadStatus.FETCHING_METADATA,
                                    title = downloadItem.title,
                                    progress = latestProgress.get(),
                                    diagnosticMessage = "Resolving audio stream.",
                                )
                                setForeground(
                                    foregroundInfo(
                                        downloadId,
                                        "Resolving audio stream",
                                        progress = null,
                                    ),
                                )

                                downloader.execute(
                                    url = downloadItem.url,
                                    outputTemplate = outputTemplate,
                                    processId = attemptProcessId,
                                ) { update ->
                                    update.line?.let { line ->
                                        lastDownloaderLine.set(line)
                                        DownloadLogger.debug(downloadId, "yt-dlp: $line")
                                    }
                                    if (update.percent >= 0f) {
                                        transferStartedAt.compareAndSet(
                                            0L,
                                            SystemClock.elapsedRealtime(),
                                        )
                                    }
                                    progressEvents.trySend(update)
                                }
                                SystemClock.elapsedRealtime()
                            }
                            DownloaderProcessRegistry.unregister(downloadId, attemptProcessId)
                            val transferStarted = transferStartedAt.get()
                            if (transferStarted > 0L) {
                                DownloadLogger.info(
                                    downloadId,
                                    "timing stream_resolution_ms=${(transferStarted - initializationFinishedAt.get()).coerceAtLeast(0L)} " +
                                        "transfer_ms=${executeFinishedAt - transferStarted}",
                                )
                            } else {
                                DownloadLogger.info(
                                    downloadId,
                                    "timing stream_resolution_ms=unknown " +
                                        "transfer_ms=${executeFinishedAt - attemptStartedAt}",
                                )
                            }
                            break
                        } catch (error: YoutubeDLException) {
                            DownloaderProcessRegistry.unregister(downloadId, attemptProcessId)
                            if (
                                repairedDownloader ||
                                !error.shouldRepairYoutube403(downloadItem.url, lastDownloaderLine.get())
                            ) {
                                throw error
                            }

                            repairedDownloader = true
                            DownloadLogger.info(
                                downloadId,
                                "YouTube 403 detected; updating stable yt-dlp before retry",
                            )
                            downloadingStarted.set(false)
                            updateState(
                                repository = downloadRepository,
                                id = downloadId,
                                status = DownloadStatus.FETCHING_METADATA,
                                title = downloadItem.title,
                                progress = latestProgress.get(),
                                diagnosticMessage = "Updating the downloader before retrying.",
                            )
                            setForeground(
                                foregroundInfo(
                                    downloadId,
                                    "Updating audio downloader",
                                    progress = null,
                                ),
                            )
                            val updateStartedAt = SystemClock.elapsedRealtime()
                            val updateMessage = DownloaderUpdater.update(
                                context = applicationContext,
                                reason = "YouTube 403 repair",
                            )
                            DownloadLogger.info(
                                downloadId,
                                "timing downloader_repair_update_ms=${SystemClock.elapsedRealtime() - updateStartedAt}",
                            )
                            lastDownloaderLine.set(null)
                            downloadAttempt += 1
                            updateState(
                                repository = downloadRepository,
                                id = downloadId,
                                status = DownloadStatus.FETCHING_METADATA,
                                title = downloadItem.title,
                                progress = latestProgress.get(),
                                diagnosticMessage = "$updateMessage. Preparing the audio stream again.",
                            )
                    }
                    }
                    operationCompletedNormally.set(true)
                } finally {
                    cancellationMonitor.cancel()
                    progressEvents.close()
                    withContext(NonCancellable) {
                        progressJob.join()
                    }
                }
            }

            val downloadedMetadata = findInfoJsonFile(outputDirectory, downloadId)
                ?.readDownloadedMetadata()
                ?: DownloadedAudioMetadata()
            val title = downloadedMetadata.title ?: downloadItem.title
            val artist = downloadedMetadata.artist
            updateState(
                repository = downloadRepository,
                id = downloadId,
                status = DownloadStatus.EXTRACTING,
                title = title,
                progress = latestProgress.get().coerceAtLeast(100f),
                diagnosticMessage = "Adding audio to your library.",
            )
            setForeground(foregroundInfo(downloadId, "Adding to library", 100f))

            val outputFile = findOutputFile(outputDirectory, downloadId)
                ?: error("Downloader finished but no audio output was found.")
            val registrationStartedAt = SystemClock.elapsedRealtime()
            val song = appMusicStorage.registerDownloadedAudio(
                file = outputFile,
                fallbackTitle = title,
                fallbackArtist = artist,
                sourceUrl = downloadItem.url,
            )
            val completed = database.withTransaction {
                val currentDownload = downloadRepository.getDownload(downloadId)
                if (currentDownload == null || currentDownload.status == DownloadStatus.CANCELLED) {
                    false
                } else {
                    if (songRepository.getSongByUri(song.uri) == null) {
                        songRepository.addSong(song)
                    }
                    downloadRepository.markCompleted(
                        id = downloadId,
                        title = song.title,
                        outputUri = song.uri,
                    )
                }
            }
            DownloadLogger.info(
                downloadId,
                "timing library_registration_ms=${SystemClock.elapsedRealtime() - registrationStartedAt}",
            )
            if (!completed) {
                DownloadLogger.info(downloadId, "Skipping library registration because the import was cancelled or removed")
                Result.success()
            } else {
                DownloadLogger.info(downloadId, "Song added and download completed title=${song.title}")
                findInfoJsonFile(outputDirectory, downloadId)?.delete()
                DownloadLogger.info(
                    downloadId,
                    "timing worker_total_ms=${SystemClock.elapsedRealtime() - workerStartedAt}",
                )
                Result.success()
            }
        } catch (error: YoutubeDL.CanceledException) {
            DownloadLogger.info(downloadId, "Downloader process cancelled")
            val currentDownload = downloadRepository.getDownload(downloadId)
            if (isStopped && currentDownload?.status != DownloadStatus.CANCELLED) {
                throw CancellationException("WorkManager stopped the import")
            }
            if (currentDownload?.status != DownloadStatus.CANCELLED) {
                updateState(
                    repository = downloadRepository,
                    id = downloadId,
                    status = DownloadStatus.CANCELLED,
                    title = currentDownload?.title,
                    progress = currentDownload?.progress ?: 0f,
                    diagnosticMessage = "Import cancelled.",
                )
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            if (isStopped && downloadRepository.getDownload(downloadId)?.status != DownloadStatus.CANCELLED) {
                throw CancellationException("WorkManager stopped the import")
            }
            handleFailure(downloadRepository, downloadId, error, lastDownloaderLine.get())
        } catch (error: Throwable) {
            handleFailure(downloadRepository, downloadId, error, lastDownloaderLine.get())
        }
    }

    private suspend fun handleFailure(
        repository: DownloadRepository,
        downloadId: Long,
        error: Throwable,
        lastDownloaderLine: String?,
    ): Result {
        val message = error.userVisibleDownloadMessage(lastDownloaderLine)
        DownloadLogger.error(downloadId, "Import failed: $message", error)
        if (error.shouldRetryDownload(lastDownloaderLine, runAttemptCount)) {
            val nextAttempt = runAttemptCount + 1
            if (repository.markRetryScheduled(downloadId, nextAttempt, MAX_DOWNLOAD_RETRIES)) {
                DownloadLogger.info(downloadId, "Transient failure; retry $nextAttempt/$MAX_DOWNLOAD_RETRIES")
                return Result.retry()
            }
        }

        repository.markFailed(downloadId, message)
        return Result.failure()
    }

    private fun findOutputFile(outputDirectory: File, downloadId: Long): File? =
        outputDirectory
            .listFiles { file ->
                file.isFile &&
                    file.name.startsWith("sona-import-$downloadId.") &&
                    !file.name.endsWith(INFO_JSON_SUFFIX) &&
                    !file.name.endsWith(PARTIAL_DOWNLOAD_SUFFIX)
            }
            ?.maxByOrNull { it.lastModified() }

    private fun findInfoJsonFile(outputDirectory: File, downloadId: Long): File? =
        outputDirectory
            .listFiles { file ->
                file.isFile &&
                    file.name.startsWith("sona-import-$downloadId.") &&
                    file.name.endsWith(INFO_JSON_SUFFIX)
            }
            ?.maxByOrNull { it.lastModified() }

    private fun File.readDownloadedMetadata(): DownloadedAudioMetadata =
        runCatching {
            val json = JSONObject(readText())
            DownloadedAudioMetadata(
                title = json.firstString("title", "fulltitle"),
                artist = json.firstString("artist", "uploader", "channel"),
            )
        }.getOrDefault(DownloadedAudioMetadata())

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

    private fun foregroundInfo(
        downloadId: Long,
        stage: String,
        progress: Float?,
    ): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    DOWNLOAD_NOTIFICATION_CHANNEL,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        val notificationBuilder = NotificationCompat.Builder(
            applicationContext,
            DOWNLOAD_NOTIFICATION_CHANNEL,
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Sona download")
            .setContentText(stage)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val percentage = progress?.coerceIn(0f, 100f)?.toInt()
        if (percentage == null) {
            notificationBuilder.setProgress(0, 0, true)
        } else {
            notificationBuilder.setProgress(100, percentage, false)
        }

        val notificationId = DOWNLOAD_NOTIFICATION_ID_BASE + (downloadId % 10_000L).toInt()
        val notification = notificationBuilder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private companion object {
        const val INFO_JSON_SUFFIX = ".info.json"
        const val PARTIAL_DOWNLOAD_SUFFIX = ".part"
        const val DOWNLOAD_NOTIFICATION_CHANNEL = "sona_downloads"
        const val DOWNLOAD_NOTIFICATION_ID_BASE = 4_000
        const val PROGRESS_PERSIST_INTERVAL_MS = 500L
    }
}

internal class UrlImportWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = if (workerClassName == UrlImportWorker::class.java.name) {
        UrlImportWorker(
            context = appContext,
            workerParameters = workerParameters,
            downloader = YoutubeDlDownloadExecutor(appContext),
        )
    } else {
        null
    }
}

private data class DownloadedAudioMetadata(
    val title: String? = null,
    val artist: String? = null,
)

private fun JSONObject.firstString(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        optString(key)
            .trim()
            .takeIf { it.isNotBlank() }
    }

private val DownloadStatus.isTerminal: Boolean
    get() = when (this) {
        DownloadStatus.COMPLETED,
        DownloadStatus.FAILED,
        DownloadStatus.CANCELLED,
        -> true
        DownloadStatus.QUEUED,
        DownloadStatus.FETCHING_METADATA,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.EXTRACTING,
        -> false
    }
