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

    private fun song(
        id: Long,
        title: String,
        isFavorite: Boolean = false,
        lastPlayedAt: Long? = null,
    ): Song = Song(
        id = id,
        title = title,
        artist = "Artist",
        durationMs = 60_000L,
        uri = "file:///music/$id.mp3",
        dateAdded = id,
        isFavorite = isFavorite,
        lastPlayedAt = lastPlayedAt,
    )
}
