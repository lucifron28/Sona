package com.example.sona.downloader

import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.domain.model.Song
import com.example.sona.domain.model.SourceType

internal suspend fun List<DownloadItem>.findReusableImport(
    isCompletedAudioAvailable: suspend (DownloadItem) -> Boolean,
): DownloadItem? {
    firstOrNull { it.status.isInProgress }?.let { return it }
    for (item in this) {
        if (item.status == DownloadStatus.COMPLETED && isCompletedAudioAvailable(item)) {
            return item
        }
    }
    return null
}

internal fun DownloadItem.hasAvailableLibraryAudio(
    song: Song?,
    outputFileAvailable: Boolean,
): Boolean {
    val expectedOutputUri = outputUri ?: return false
    return status == DownloadStatus.COMPLETED &&
        song != null &&
        song.uri == expectedOutputUri &&
        song.sourceType == SourceType.IMPORTED_URL &&
        song.sourceUrl?.normalizedDownloadUrl() == url &&
        outputFileAvailable
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
