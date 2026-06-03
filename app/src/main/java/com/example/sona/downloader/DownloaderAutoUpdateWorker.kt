package com.example.sona.downloader

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import java.util.concurrent.TimeUnit

class DownloaderAutoUpdateWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        if (!DownloaderUpdater.shouldAutoUpdate(applicationContext)) {
            DownloadLogger.info(null, "Skipping downloader auto-update; already fresh")
            return Result.success()
        }

        return runCatching {
            DownloaderUpdater.update(
                context = applicationContext,
                channel = YoutubeDL.UpdateChannel.MASTER,
                reason = "background auto-update",
            )
        }.fold(
            onSuccess = { message ->
                DownloadLogger.info(null, "Downloader auto-update finished: $message")
                Result.success()
            },
            onFailure = { error ->
                DownloadLogger.error(null, "Downloader auto-update failed", error)
                Result.retry()
            },
        )
    }
}

object DownloaderAutoUpdateScheduler {
    private const val STARTUP_WORK_NAME = "downloader-auto-update-startup"
    private const val PERIODIC_WORK_NAME = "downloader-auto-update-periodic"

    fun schedule(context: Context) {
        val applicationContext = context.applicationContext
        val workManager = WorkManager.getInstance(applicationContext)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val startupRequest = OneTimeWorkRequestBuilder<DownloaderAutoUpdateWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            STARTUP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            startupRequest,
        )

        val periodicRequest = PeriodicWorkRequestBuilder<DownloaderAutoUpdateWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        )
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}
