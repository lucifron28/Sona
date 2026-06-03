package com.example.sona.data.entities

import com.example.sona.domain.model.Song
import com.example.sona.domain.model.SourceType

fun SongEntity.toSong(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    dateAdded = dateAdded,
    sourceType = sourceType.toSourceType(),
    sourceUrl = sourceUrl,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    isFavorite = isFavorite,
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    dateAdded = dateAdded,
    sourceType = sourceType.name,
    sourceUrl = sourceUrl,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    isFavorite = isFavorite,
)

fun String.toSourceType(): SourceType = runCatching {
    SourceType.valueOf(this)
}.getOrDefault(SourceType.LOCAL_FILE)
