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

fun searchSongsForLibrary(
    songs: List<Song>,
    query: String,
): List<Song> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return songs

    return songs.filter { song ->
        song.title.contains(normalizedQuery, ignoreCase = true) ||
            song.artist.contains(normalizedQuery, ignoreCase = true) ||
            song.album?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

fun artistGroupsForLibrary(songs: List<Song>): List<LibraryGroup> =
    songs
        .groupBy { song -> song.artist.trim().ifBlank { UNKNOWN_ARTIST } }
        .toSortedGroups { artist, artistSongs ->
            val albumCount = artistSongs
                .mapNotNull { it.album?.trim()?.takeIf(String::isNotBlank) }
                .distinctBy { it.lowercase() }
                .size
            LibraryGroup(
                name = artist,
                subtitle = buildCountSubtitle(
                    songCount = artistSongs.size,
                    detailCount = albumCount,
                    detailSingular = "album",
                    detailPlural = "albums",
                ),
                songs = artistSongs,
            )
        }

fun albumGroupsForLibrary(songs: List<Song>): List<LibraryGroup> =
    songs
        .groupBy { song -> song.album?.trim()?.ifBlank { null } ?: UNKNOWN_ALBUM }
        .toSortedGroups { album, albumSongs ->
            val artistCount = albumSongs
                .map { it.artist.trim().ifBlank { UNKNOWN_ARTIST } }
                .distinctBy { it.lowercase() }
                .size
            LibraryGroup(
                name = album,
                subtitle = buildCountSubtitle(
                    songCount = albumSongs.size,
                    detailCount = artistCount,
                    detailSingular = "artist",
                    detailPlural = "artists",
                ),
                songs = albumSongs,
            )
        }

private fun Map<String, List<Song>>.toSortedGroups(
    transform: (String, List<Song>) -> LibraryGroup,
): List<LibraryGroup> =
    entries
        .sortedBy { (name, _) -> name.lowercase() }
        .map { (name, songs) -> transform(name, songs.sortedBy { it.title.lowercase() }) }

private fun buildCountSubtitle(
    songCount: Int,
    detailCount: Int,
    detailSingular: String,
    detailPlural: String,
): String {
    val songLabel = if (songCount == 1) "song" else "songs"
    val detailLabel = if (detailCount == 1) detailSingular else detailPlural
    return "$songCount $songLabel - $detailCount $detailLabel"
}

private const val UNKNOWN_ARTIST = "Unknown artist"
private const val UNKNOWN_ALBUM = "Unknown album"
