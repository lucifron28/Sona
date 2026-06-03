package com.example.sona.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val dateAdded: Long,
    val sourceType: SourceType = SourceType.LOCAL_FILE,
    val sourceUrl: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false,
)
