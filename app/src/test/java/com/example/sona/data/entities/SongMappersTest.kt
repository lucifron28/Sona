package com.example.sona.data.entities

import com.example.sona.domain.model.Song
import com.example.sona.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongMappersTest {
    @Test
    fun toSong_mapsEntityFields() {
        val entity = SongEntity(
            id = 7L,
            title = "Sona Track",
            artist = "Ron",
            album = "Learning",
            durationMs = 180_000L,
            uri = "file:///music/sona.mp3",
            artworkUri = "file:///music/sona.jpg",
            dateAdded = 123L,
            sourceType = SourceType.IMPORTED_URL.name,
            sourceUrl = "https://example.com/audio",
            playCount = 4,
            lastPlayedAt = 456L,
            isFavorite = true,
        )

        val song = entity.toSong()

        assertEquals(7L, song.id)
        assertEquals("Sona Track", song.title)
        assertEquals("Ron", song.artist)
        assertEquals("Learning", song.album)
        assertEquals(180_000L, song.durationMs)
        assertEquals("file:///music/sona.mp3", song.uri)
        assertEquals("file:///music/sona.jpg", song.artworkUri)
        assertEquals(123L, song.dateAdded)
        assertEquals(SourceType.IMPORTED_URL, song.sourceType)
        assertEquals("https://example.com/audio", song.sourceUrl)
        assertEquals(4, song.playCount)
        assertEquals(456L, song.lastPlayedAt)
        assertEquals(true, song.isFavorite)
    }

    @Test
    fun toSong_defaultsUnknownSourceTypeToLocalFile() {
        val entity = SongEntity(
            title = "Unknown source",
            artist = "Unknown artist",
            durationMs = 0L,
            uri = "file:///music/unknown.mp3",
            dateAdded = 123L,
            sourceType = "OLD_VALUE",
        )

        assertEquals(SourceType.LOCAL_FILE, entity.toSong().sourceType)
    }

    @Test
    fun toEntity_mapsSongFields() {
        val song = Song(
            id = 9L,
            title = "Manual Track",
            artist = "Sona",
            album = null,
            durationMs = 90_000L,
            uri = "file:///music/manual.flac",
            artworkUri = null,
            dateAdded = 789L,
            sourceType = SourceType.MANUAL,
            sourceUrl = null,
            playCount = 2,
            lastPlayedAt = null,
            isFavorite = false,
        )

        val entity = song.toEntity()

        assertEquals(9L, entity.id)
        assertEquals("Manual Track", entity.title)
        assertEquals("Sona", entity.artist)
        assertNull(entity.album)
        assertEquals(90_000L, entity.durationMs)
        assertEquals("file:///music/manual.flac", entity.uri)
        assertNull(entity.artworkUri)
        assertEquals(789L, entity.dateAdded)
        assertEquals(SourceType.MANUAL.name, entity.sourceType)
        assertNull(entity.sourceUrl)
        assertEquals(2, entity.playCount)
        assertNull(entity.lastPlayedAt)
        assertEquals(false, entity.isFavorite)
    }
}
