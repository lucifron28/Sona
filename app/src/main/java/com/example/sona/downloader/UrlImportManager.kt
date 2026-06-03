package com.example.sona.downloader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sona.data.repository.DownloadRepository
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlImportManager(
    context: Context,
    private val downloadRepository: DownloadRepository,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val applicationContext = context.applicationContext

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

    suspend fun updateDownloader(): String = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().init(applicationContext)
        FFmpeg.getInstance().init(applicationContext)

        val status = YoutubeDL.getInstance().updateYoutubeDL(
            applicationContext,
            YoutubeDL.UpdateChannel.STABLE,
        )
        val statusLabel = status?.name?.lowercase()?.replace('_', ' ') ?: "updated"
        "Downloader $statusLabel"
    }
}
