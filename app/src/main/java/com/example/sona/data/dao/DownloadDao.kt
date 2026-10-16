package com.example.sona.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sona.data.entities.DownloadItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun getDownload(id: Long): DownloadItemEntity?

    @Query(
        """
        SELECT * FROM download_items
        WHERE url = :url
          AND status IN ('QUEUED', 'FETCHING_METADATA', 'DOWNLOADING', 'EXTRACTING', 'COMPLETED')
        ORDER BY
            CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END,
            createdAt DESC,
            id DESC
        """,
    )
    suspend fun findImportCandidates(url: String): List<DownloadItemEntity>

    @Insert
    suspend fun insertDownload(downloadItem: DownloadItemEntity): Long

    @Query(
        """
        UPDATE download_items
        SET status = :status,
            title = COALESCE(:title, title),
            progress = :progress,
            outputUri = :outputUri,
            errorMessage = :errorMessage,
            diagnosticMessage = :diagnosticMessage,
            completedAt = :completedAt
        WHERE id = :id
          AND status NOT IN ('COMPLETED', 'CANCELLED')
        """,
    )
    suspend fun updateDownloadState(
        id: Long,
        status: String,
        title: String?,
        progress: Float,
        outputUri: String?,
        errorMessage: String?,
        diagnosticMessage: String?,
        completedAt: Long?,
    ): Int

    @Query(
        """
        UPDATE download_items
        SET progress = :progress,
            title = COALESCE(:title, title),
            diagnosticMessage = COALESCE(:diagnosticMessage, diagnosticMessage)
        WHERE id = :id AND status = 'DOWNLOADING'
        """,
    )
    suspend fun updateDownloadProgress(
        id: Long,
        progress: Float,
        title: String?,
        diagnosticMessage: String?,
    ): Int


    @Query(
        """
        UPDATE download_items
        SET workId = :workId,
            diagnosticMessage = CASE
                WHEN status = 'QUEUED' THEN :diagnosticMessage
                ELSE diagnosticMessage
            END
        WHERE id = :id
        """,
    )
    suspend fun markWorkEnqueued(
        id: Long,
        workId: String,
        diagnosticMessage: String,
    )

    @Query(
        """
        UPDATE download_items
        SET status = 'CANCELLED',
            errorMessage = NULL,
            diagnosticMessage = 'Cancelled by user',
            completedAt = NULL
        WHERE id = :id
          AND status IN ('QUEUED', 'FETCHING_METADATA', 'DOWNLOADING', 'EXTRACTING')
        """,
    )
    suspend fun cancelDownload(id: Long): Int

    @Query(
        """
        UPDATE download_items
        SET status = 'QUEUED',
            progress = 0,
            workId = NULL,
            outputUri = NULL,
            errorMessage = NULL,
            diagnosticMessage = 'Retry queued. Waiting for WorkManager.',
            completedAt = NULL
        WHERE id = :id AND status IN ('FAILED', 'CANCELLED')
        """,
    )
    suspend fun retryDownload(id: Long): Int

    @Query(
        """
        UPDATE download_items
        SET status = 'QUEUED',
            errorMessage = NULL,
            diagnosticMessage = :diagnosticMessage
        WHERE id = :id AND status NOT IN ('COMPLETED', 'CANCELLED')
        """,
    )
    suspend fun markRetryScheduled(id: Long, diagnosticMessage: String): Int

    @Query(
        """
        DELETE FROM download_items
        WHERE id = :id AND status IN ('COMPLETED', 'FAILED', 'CANCELLED')
        """,
    )
    suspend fun deleteTerminalDownload(id: Long): Int
}
