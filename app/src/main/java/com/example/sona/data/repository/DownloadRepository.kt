package com.example.sona.data.repository

import com.example.sona.data.dao.DownloadDao
import com.example.sona.data.entities.DownloadItemEntity
import com.example.sona.data.entities.toDownloadItem
import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    val downloads: Flow<List<DownloadItem>> = downloadDao.observeDownloads()
        .map { downloads -> downloads.map { it.toDownloadItem() } }

    suspend fun getDownload(id: Long): DownloadItem? =
        downloadDao.getDownload(id)?.toDownloadItem()

    suspend fun findImportCandidates(url: String): List<DownloadItem> =
        downloadDao.findImportCandidates(url).map { it.toDownloadItem() }

    suspend fun enqueue(url: String): Long = downloadDao.insertDownload(
        DownloadItemEntity(
            url = url,
            status = DownloadStatus.QUEUED.name,
            diagnosticMessage = "Created download row. Waiting for WorkManager.",
            createdAt = now(),
        ),
    )

    suspend fun markWorkEnqueued(
        id: Long,
        workId: String,
    ) {
        downloadDao.markWorkEnqueued(
            id = id,
            workId = workId,
            diagnosticMessage = "Queued in WorkManager.",
        )
    }

    suspend fun updateState(
        id: Long,
        status: DownloadStatus,
        title: String? = null,
        progress: Float = 0f,
        outputUri: String? = null,
        errorMessage: String? = null,
        diagnosticMessage: String? = null,
        completedAt: Long? = null,
    ) {
        downloadDao.updateDownloadState(
            id = id,
            status = status.name,
            title = title,
            progress = progress,
            outputUri = outputUri,
            errorMessage = errorMessage,
            diagnosticMessage = diagnosticMessage,
            completedAt = completedAt,
        )
    }

    suspend fun updateProgress(
        id: Long,
        progress: Float,
        title: String?,
        diagnosticMessage: String?,
    ) {
        downloadDao.updateDownloadProgress(
            id = id,
            progress = progress.coerceIn(0f, 100f),
            title = title,
            diagnosticMessage = diagnosticMessage,
        )
    }

    suspend fun markCompleted(
        id: Long,
        title: String?,
        outputUri: String,
    ): Boolean = downloadDao.updateDownloadState(
        id = id,
        status = DownloadStatus.COMPLETED.name,
        title = title,
        progress = 100f,
        outputUri = outputUri,
        errorMessage = null,
        diagnosticMessage = "Import complete. Added to library.",
        completedAt = now(),
    ) > 0

    suspend fun markRetryScheduled(id: Long, attempt: Int, maxRetries: Int): Boolean =
        downloadDao.markRetryScheduled(
            id = id,
            diagnosticMessage = "Temporary network issue. Retry $attempt of $maxRetries scheduled.",
        ) > 0

    suspend fun markFailed(id: Long, errorMessage: String) {
        updateState(
            id = id,
            status = DownloadStatus.FAILED,
            errorMessage = errorMessage,
            diagnosticMessage = "Import failed.",
        )
    }

    suspend fun cancelDownload(id: Long): Boolean =
        downloadDao.cancelDownload(id) > 0

    suspend fun retryDownload(id: Long): Boolean =
        downloadDao.retryDownload(id) > 0

    suspend fun removeHistory(id: Long): Boolean =
        downloadDao.deleteTerminalDownload(id) > 0
}
