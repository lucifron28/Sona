package com.example.sona.ui.library

import com.example.sona.domain.model.Song
import com.example.sona.domain.model.Playlist

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val artistGroups: List<LibraryGroup> = emptyList(),
    val albumGroups: List<LibraryGroup> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedView: LibraryView = LibraryView.SONGS,
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val selectedGroup: SelectedLibraryGroup? = null,
    val editState: TrackEditState? = null,
    val trackActionsState: TrackActionsState? = null,
    val deleteConfirmationSong: Song? = null,
)

data class LibraryGroup(
    val name: String,
    val subtitle: String,
    val songs: List<Song>,
)

data class SelectedLibraryGroup(
    val view: LibraryView,
    val group: LibraryGroup,
)

data class TrackEditState(
    val song: Song,
    val title: String,
    val artist: String,
    val artistSuggestions: List<String>,
)

data class TrackActionsState(
    val song: Song,
    val playlists: List<Playlist>,
)
