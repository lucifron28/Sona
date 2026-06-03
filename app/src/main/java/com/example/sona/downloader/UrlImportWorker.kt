package com.example.sona.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.storage.AppMusicStorage
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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
        if (downloadId <= 0L) return Result.failure()

        val database = SonaDatabase.create(applicationContext)
        val downloadRepository = DownloadRepository(database.downloadDao())
        val songRepository = SongRepository(database.songDao())
        val appMusicStorage = AppMusicStorage(applicationContext)
        val downloadItem = downloadRepository.getDownload(downloadId) ?: return Result.failure()

        return try {
            downloadRepository.updateState(downloadId, DownloadStatus.FETCHING_METADATA)
            YoutubeDL.getInstance().init(applicationContext)
            FFmpeg.getInstance().init(applicationContext)

            val videoInfo = YoutubeDL.getInstance().getInfo(downloadItem.url)
            val title = videoInfo.title ?: videoInfo.fulltitle
            val artist = videoInfo.uploader

            downloadRepository.updateState(
                id = downloadId,
                status = DownloadStatus.DOWNLOADING,
                title = title,
            )

            val outputDirectory = File(applicationContext.filesDir, "music").apply {
                mkdirs()
            }
            val outputTemplate = File(outputDirectory, "sona-import-$downloadId.%(ext)s")
            val request = YoutubeDLRequest(downloadItem.url).apply {
                addOption("--no-playlist")
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
                addOption("--audio-quality", "0")
                addOption("--newline")
                addOption("--no-mtime")
                addOption("-o", outputTemplate.absolutePath)
            }

            YoutubeDL.getInstance().execute(request, processId, true) { progress, _, _ ->
                runBlocking {
                    downloadRepository.updateState(
                        id = downloadId,
                        status = DownloadStatus.DOWNLOADING,
                        title = title,
                        progress = progress.coerceIn(0f, 100f),
                    )
                }
            }

            downloadRepository.updateState(
                id = downloadId,
                status = DownloadStatus.EXTRACTING,
                title = title,
                progress = 95f,
            )

            val outputFile = findOutputFile(outputDirectory, downloadId)
                ?: error("Downloader finished but no audio output was found.")
            val song = appMusicStorage.registerDownloadedAudio(
                file = outputFile,
                fallbackTitle = title,
                fallbackArtist = artist,
                sourceUrl = downloadItem.url,
            )
            songRepository.addSong(song)
            downloadRepository.markCompleted(
                id = downloadId,
                title = song.title,
                outputUri = song.uri,
            )

            Result.success()
        } catch (error: YoutubeDL.CanceledException) {
            downloadRepository.updateState(downloadId, DownloadStatus.CANCELLED)
            Result.failure()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            downloadRepository.markFailed(downloadId, "Import interrupted.")
            Result.failure()
        } catch (error: Throwable) {
            downloadRepository.markFailed(
                id = downloadId,
                errorMessage = error.message ?: "Import failed.",
            )
            Result.failure()
        } finally {
            database.close()
        }
    }

    private fun findOutputFile(outputDirectory: File, downloadId: Long): File? =
        outputDirectory
            .listFiles { file -> file.isFile && file.name.startsWith("sona-import-$downloadId.") }
            ?.maxByOrNull { it.lastModified() }
}
