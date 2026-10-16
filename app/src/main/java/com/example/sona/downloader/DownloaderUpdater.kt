package com.example.sona.downloader

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

object DownloaderUpdater {
    const val AUTO_UPDATE_INTERVAL_MS = 24L * 60L * 60L * 1000L
    const val PREFS_NAME = "sona_downloader_updater"
    const val KEY_LAST_UPDATE_AT = "last_update_at"

    suspend fun update(
        context: Context,
        reason: String,
        now: () -> Long = { System.currentTimeMillis() },
    ): String = withContext(Dispatchers.IO) {
        DownloaderOperationLock.mutex.withLock {
            val applicationContext = context.applicationContext
            DownloadLogger.info(null, "Initializing downloader for $reason")
            YoutubeDL.getInstance().init(applicationContext)
            FFmpeg.getInstance().init(applicationContext)

            DownloadLogger.info(
                null,
                "Updating downloader channel=${UPDATE_CHANNEL::class.simpleName} reason=$reason",
            )
            val status = YoutubeDL.getInstance().updateYoutubeDL(applicationContext, UPDATE_CHANNEL)
            val statusLabel = status?.name?.lowercase()?.replace('_', ' ') ?: "updated"
            applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_UPDATE_AT, now())
                .apply()
            DownloadLogger.info(null, "Downloader update result=$statusLabel reason=$reason")

            "Downloader $statusLabel"
        }
    }

    fun shouldAutoUpdate(
        context: Context,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val lastUpdateAt = context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE_AT, 0L)

        return isUpdateStale(
            lastUpdateAt = lastUpdateAt,
            now = now,
            intervalMs = AUTO_UPDATE_INTERVAL_MS,
        )
    }
    private val UPDATE_CHANNEL = YoutubeDL.UpdateChannel.STABLE
}

fun isUpdateStale(
    lastUpdateAt: Long,
    now: Long,
    intervalMs: Long,
): Boolean = lastUpdateAt <= 0L || now - lastUpdateAt >= intervalMs
