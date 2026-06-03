package com.example.sona.downloader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sona.data.repository.DownloadRepository

class UrlImportManager(
    context: Context,
    private val downloadRepository: DownloadRepository,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    suspend fun enqueue(url: String): Long {
        val downloadId = downloadRepository.enqueue(url.trim())
        val request = OneTimeWorkRequestBuilder<UrlImportWorker>()
            .setInputData(workDataOf(KEY_DOWNLOAD_ID to downloadId))
            .build()

        workManager.enqueueUniqueWork(
            "url-import-$downloadId",
            ExistingWorkPolicy.REPLACE,
            request,
        )

        return downloadId
    }
}
