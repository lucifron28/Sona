package com.example.sona.ui.library

import com.example.sona.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySongFiltersTest {
    @Test
    fun filterSongsForLibrary_returnsAllSongs() {
        val songs = listOf(
            song(id = 1L, title = "One"),
            song(id = 2L, title = "Two", isFavorite = true),
        )

        assertEquals(songs, filterSongsForLibrary(songs, LibraryFilter.ALL))
    }

    @Test
    fun filterSongsForLibrary_returnsOnlyFavorites() {
        val songs = listOf(
            song(id = 1L, title = "One"),
            song(id = 2L, title = "Two", isFavorite = true),
        )

        val filtered = filterSongsForLibrary(songs, LibraryFilter.FAVORITES)

        assertEquals(listOf(songs[1]), filtered)
    }

    @Test
    fun filterSongsForLibrary_returnsRecentlyPlayedNewestFirst() {
        val older = song(id = 1L, title = "Older", lastPlayedAt = 10L)
        val neverPlayed = song(id = 2L, title = "Never")
        val newer = song(id = 3L, title = "Newer", lastPlayedAt = 20L)

        val filtered = filterSongsForLibrary(
            songs = listOf(older, neverPlayed, newer),
            filter = LibraryFilter.RECENT,
        )

        assertEquals(listOf(newer, older), filtered)
    }

    @Test
    fun searchSongsForLibrary_matchesTitleArtistAndAlbum() {
        val titleMatch = song(id = 1L, title = "Night Lights", artist = "A", album = "One")
        val artistMatch = song(id = 2L, title = "Two", artist = "Night Circuits", album = "Two")
        val albumMatch = song(id = 3L, title = "Three", artist = "B", album = "Night Drive")
        val miss = song(id = 4L, title = "Morning", artist = "C", album = "Sun")

        assertEquals(
            listOf(titleMatch, artistMatch, albumMatch),
            searchSongsForLibrary(
                songs = listOf(titleMatch, artistMatch, albumMatch, miss),
                query = "night",
            ),
        )
    }

    @Test
    fun artistGroupsForLibrary_groupsSongsByArtist() {
        val first = song(id = 1L, title = "B", artist = "Ron", album = "Blue")
        val second = song(id = 2L, title = "A", artist = "Ron", album = "Red")
        val other = song(id = 3L, title = "C", artist = "Sona", album = "Red")

        val groups = artistGroupsForLibrary(listOf(first, second, other))

        assertEquals(listOf("Ron", "Sona"), groups.map { it.name })
        assertEquals(listOf(second, first), groups.first().songs)
        assertEquals("2 songs - 2 albums", groups.first().subtitle)
    }

    @Test
    fun albumGroupsForLibrary_usesUnknownAlbumFallback() {
        val unknown = song(id = 1L, title = "One", artist = "Ron", album = null)

        val groups = albumGroupsForLibrary(listOf(unknown))

        assertEquals("Unknown album", groups.single().name)
        assertEquals("1 song - 1 artist", groups.single().subtitle)
    }

    private fun song(
        id: Long,
        title: String,
        artist: String = "Artist",
        album: String? = null,
        isFavorite: Boolean = false,
        lastPlayedAt: Long? = null,
    ): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 60_000L,
        uri = "file:///music/$id.mp3",
        dateAdded = id,
        isFavorite = isFavorite,
        lastPlayedAt = lastPlayedAt,
    )
}
