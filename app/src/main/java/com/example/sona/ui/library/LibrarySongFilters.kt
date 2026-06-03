package com.example.sona.ui.library

import com.example.sona.domain.model.Song

fun filterSongsForLibrary(
    songs: List<Song>,
    filter: LibraryFilter,
): List<Song> = when (filter) {
    LibraryFilter.ALL -> songs
    LibraryFilter.FAVORITES -> songs.filter { it.isFavorite }
    LibraryFilter.RECENT -> songs
        .filter { it.lastPlayedAt != null }
        .sortedByDescending { it.lastPlayedAt }
}
