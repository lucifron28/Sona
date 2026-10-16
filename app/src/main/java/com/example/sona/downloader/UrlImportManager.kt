package com.example.sona.downloader

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.storage.AppMusicStorage
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UrlImportManager(
    context: Context,
    private val downloadRepository: DownloadRepository,
    private val songRepository: SongRepository,
    private val appMusicStorage: AppMusicStorage,
) {
    private val applicationContext = context.applicationContext
    private val workManager = WorkManager.getInstance(applicationContext)
    private val enqueueMutex = Mutex()

    suspend fun enqueue(url: String): Long = enqueueMutex.withLock {
        val normalizedUrl = url.normalizedDownloadUrl()
        val candidates = downloadRepository.findImportCandidates(normalizedUrl)
        candidates.findReusableImport { item ->
            item.hasAvailableDownloadedAudio()
        }?.let { return@withLock it.id }

        val downloadId = downloadRepository.enqueue(normalizedUrl)
        enqueueWork(downloadId)
        downloadId
    }

    suspend fun cancel(id: Long): Boolean {
        val download = downloadRepository.getDownload(id) ?: return false
        if (!download.status.isInProgress) return false

        val cancelled = downloadRepository.cancelDownload(id)
        if (!cancelled) return false
        DownloaderProcessRegistry.cancel(id)

        download.workId
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let(workManager::cancelWorkById)
        return true
    }

    suspend fun retry(id: Long): Boolean = enqueueMutex.withLock {
        val download = downloadRepository.getDownload(id) ?: return@withLock false
        if (download.status != DownloadStatus.FAILED && download.status != DownloadStatus.CANCELLED) {
            return@withLock false
        }
        if (!downloadRepository.retryDownload(id)) return@withLock false

        enqueueWork(id)
        true
    }

    suspend fun removeHistory(id: Long): Boolean =
        downloadRepository.removeHistory(id)

    suspend fun updateDownloader(): String = DownloaderUpdater.update(
        context = applicationContext,
        reason = "manual update",
    )

    private suspend fun DownloadItem.hasAvailableDownloadedAudio(): Boolean {
        val outputUri = outputUri ?: return false
        val song = songRepository.getSongByUri(outputUri) ?: return false
        val outputFileAvailable = appMusicStorage.isDownloadedAudioAvailable(outputUri)
        return hasAvailableLibraryAudio(
            song = song,
            outputFileAvailable = outputFileAvailable,
        )
    }

    private suspend fun enqueueWork(downloadId: Long) {
        val request = OneTimeWorkRequestBuilder<UrlImportWorker>()
            .setInputData(workDataOf(KEY_DOWNLOAD_ID to downloadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_BACKOFF_MS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        downloadRepository.markWorkEnqueued(downloadId, request.id.toString())

        workManager.enqueueUniqueWork(
            uniqueWorkName(downloadId),
            ExistingWorkPolicy.KEEP,
            request,
        )

        if (downloadRepository.getDownload(downloadId)?.status == DownloadStatus.CANCELLED) {
            workManager.cancelWorkById(request.id)
        }
    }

    private companion object {
        const val IMPORT_WORK_PREFIX = "sona-url-import-"
        const val RETRY_BACKOFF_MS = 15_000L

        fun uniqueWorkName(downloadId: Long): String = "$IMPORT_WORK_PREFIX$downloadId"
    }
}

private val DownloadStatus.isInProgress: Boolean
    get() = when (this) {
        DownloadStatus.QUEUED,
        DownloadStatus.FETCHING_METADATA,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.EXTRACTING,
        -> true
        DownloadStatus.COMPLETED,
        DownloadStatus.FAILED,
        DownloadStatus.CANCELLED,
        -> false
    }
