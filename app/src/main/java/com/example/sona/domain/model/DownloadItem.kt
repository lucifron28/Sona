package com.example.sona.domain.model

data class DownloadItem(
    val id: Long = 0L,
    val url: String,
    val title: String? = null,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val workId: String? = null,
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val diagnosticMessage: String? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
)
