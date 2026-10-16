package com.example.sona.downloader

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal data class DownloaderProgress(
    val percent: Float,
    val line: String?,
)

internal interface UrlDownloadExecutor {
    suspend fun initialize()

    suspend fun execute(
        url: String,
        outputTemplate: File,
        processId: String,
        onProgress: (DownloaderProgress) -> Unit,
    )

    fun cancel(processId: String)
}

internal class YoutubeDlDownloadExecutor(
    private val context: Context,
) : UrlDownloadExecutor {
    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().init(context.applicationContext)
        }
    }

    override suspend fun execute(
        url: String,
        outputTemplate: File,
        processId: String,
        onProgress: (DownloaderProgress) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().execute(
                audioDownloadRequest(url, outputTemplate),
                processId,
                true,
            ) { progress, _, line ->
                onProgress(
                    DownloaderProgress(
                        percent = progress,
                        line = line.cleanedDownloaderLine(),
                    ),
                )
            }
        }
    }

    override fun cancel(processId: String) {
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }.onFailure { error ->
            DownloadLogger.error(null, "Unable to cancel downloader processId=$processId", error)
        }
    }

    private fun audioDownloadRequest(url: String, outputTemplate: File): YoutubeDLRequest =
        YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--continue")
            addOption("-f", AUDIO_FORMAT_SELECTOR)
            addOption("--newline")
            addOption("--no-mtime")
            addOption("--write-info-json")
            addOption("-o", outputTemplate.absolutePath)
        }

    private companion object {
        const val AUDIO_FORMAT_SELECTOR = "bestaudio[ext=m4a]/bestaudio/best"
    }
}

internal object DownloaderOperationLock {
    val mutex = Mutex()
}

internal object DownloaderProcessRegistry {
    private data class ActiveProcess(
        val processId: String,
        val executor: UrlDownloadExecutor,
    )

    private val activeProcesses = java.util.concurrent.ConcurrentHashMap<Long, ActiveProcess>()

    fun register(downloadId: Long, processId: String, executor: UrlDownloadExecutor) {
        activeProcesses[downloadId] = ActiveProcess(processId, executor)
    }

    fun unregister(downloadId: Long, processId: String) {
        val activeProcess = activeProcesses[downloadId] ?: return
        if (activeProcess.processId == processId) {
            activeProcesses.remove(downloadId, activeProcess)
        }
    }

    fun cancel(downloadId: Long) {
        activeProcesses.remove(downloadId)?.let { process ->
            process.executor.cancel(process.processId)
        }
    }
}
