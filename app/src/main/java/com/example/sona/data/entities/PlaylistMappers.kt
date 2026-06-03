package com.example.sona.data.entities

import com.example.sona.domain.model.Playlist

fun PlaylistEntity.toPlaylist(): Playlist = Playlist(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
