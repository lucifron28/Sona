package com.example.sona.ui.downloads

import com.example.sona.domain.model.DownloadItem

data class DownloadsUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val url: String = "",
    val isUpdatingDownloader: Boolean = false,
    val message: String? = null,
)
