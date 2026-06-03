package com.example.sona.data.entities

import com.example.sona.domain.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistMappersTest {
    @Test
    fun toPlaylist_mapsEntityFields() {
        val entity = PlaylistEntity(
            id = 3L,
            name = "Road trip",
            createdAt = 100L,
            updatedAt = 200L,
        )

        val playlist = entity.toPlaylist()

        assertEquals(3L, playlist.id)
        assertEquals("Road trip", playlist.name)
        assertEquals(100L, playlist.createdAt)
        assertEquals(200L, playlist.updatedAt)
    }

    @Test
    fun toEntity_mapsPlaylistFields() {
        val playlist = Playlist(
            id = 4L,
            name = "Night queue",
            createdAt = 300L,
            updatedAt = 400L,
        )

        val entity = playlist.toEntity()

        assertEquals(4L, entity.id)
        assertEquals("Night queue", entity.name)
        assertEquals(300L, entity.createdAt)
        assertEquals(400L, entity.updatedAt)
    }
}
