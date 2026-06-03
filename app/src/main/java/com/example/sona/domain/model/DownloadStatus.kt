package com.example.sona.domain.model

enum class DownloadStatus {
    QUEUED,
    FETCHING_METADATA,
    DOWNLOADING,
    EXTRACTING,
    COMPLETED,
    FAILED,
    CANCELLED,
}
