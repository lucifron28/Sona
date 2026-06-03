package com.example.sona.downloader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sona.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL

class UrlImportManager(
    context: Context,
    private val downloadRepository: DownloadRepository,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val applicationContext = context.applicationContext

    suspend fun enqueue(url: String): Long {
        val trimmedUrl = url.trim()
        DownloadLogger.info(null, "Creating URL import for $trimmedUrl")
        val downloadId = downloadRepository.enqueue(trimmedUrl)
        val request = OneTimeWorkRequestBuilder<UrlImportWorker>()
            .setInputData(workDataOf(KEY_DOWNLOAD_ID to downloadId))
            .build()
        DownloadLogger.info(downloadId, "Enqueuing WorkManager request workId=${request.id}")

        workManager.enqueueUniqueWork(
            "url-import-$downloadId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        downloadRepository.markWorkEnqueued(downloadId, request.id.toString())
        DownloadLogger.info(downloadId, "WorkManager request accepted")

        return downloadId
    }

    suspend fun updateDownloader(): String = DownloaderUpdater.update(
        context = applicationContext,
        channel = YoutubeDL.UpdateChannel.STABLE,
        reason = "manual update",
    )
}
