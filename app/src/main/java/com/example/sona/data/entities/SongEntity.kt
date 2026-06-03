package com.example.sona.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val dateAdded: Long,
    val sourceType: String,
    val sourceUrl: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false,
)
