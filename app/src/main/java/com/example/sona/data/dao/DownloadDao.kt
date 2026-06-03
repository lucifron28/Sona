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
    )

    @Query(
        """
        UPDATE download_items
        SET workId = :workId,
            diagnosticMessage = :diagnosticMessage
        WHERE id = :id
        """,
    )
    suspend fun markWorkEnqueued(
        id: Long,
        workId: String,
        diagnosticMessage: String,
    )

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun deleteDownload(id: Long)
}
