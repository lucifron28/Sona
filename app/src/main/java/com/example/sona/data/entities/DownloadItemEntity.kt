package com.example.sona.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val url: String,
    val title: String? = null,
    val status: String,
    val progress: Float = 0f,
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
)
