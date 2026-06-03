package com.example.sona.ui.downloads

import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus

data class DownloadsUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val url: String = "",
    val isUpdatingDownloader: Boolean = false,
    val message: String? = null,
    val snackbarEvent: DownloadSnackbarEvent? = null,
)

data class DownloadSnackbarEvent(
    val id: String,
    val message: String,
    val isError: Boolean,
)

fun DownloadItem.toSnackbarEvent(): DownloadSnackbarEvent? = when (status) {
    DownloadStatus.COMPLETED -> DownloadSnackbarEvent(
        id = "$id-${status.name}",
        message = "Track downloaded: ${title ?: "Imported track"}",
        isError = false,
    )
    DownloadStatus.FAILED,
    DownloadStatus.CANCELLED,
    -> DownloadSnackbarEvent(
        id = "$id-${status.name}",
        message = errorMessage ?: "Track did not download.",
        isError = true,
    )
    DownloadStatus.QUEUED,
    DownloadStatus.FETCHING_METADATA,
    DownloadStatus.DOWNLOADING,
    DownloadStatus.EXTRACTING,
    -> null
}
