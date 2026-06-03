package com.example.sona.data.entities

import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus

fun DownloadItemEntity.toDownloadItem(): DownloadItem = DownloadItem(
    id = id,
    url = url,
    title = title,
    status = status.toDownloadStatus(),
    progress = progress,
    workId = workId,
    outputUri = outputUri,
    errorMessage = errorMessage,
    diagnosticMessage = diagnosticMessage,
    createdAt = createdAt,
    completedAt = completedAt,
)

fun DownloadItem.toEntity(): DownloadItemEntity = DownloadItemEntity(
    id = id,
    url = url,
    title = title,
    status = status.name,
    progress = progress,
    workId = workId,
    outputUri = outputUri,
    errorMessage = errorMessage,
    diagnosticMessage = diagnosticMessage,
    createdAt = createdAt,
    completedAt = completedAt,
)

fun String.toDownloadStatus(): DownloadStatus = runCatching {
    DownloadStatus.valueOf(this)
}.getOrDefault(DownloadStatus.FAILED)
