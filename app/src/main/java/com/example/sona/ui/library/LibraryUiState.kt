package com.example.sona.ui.library

import com.example.sona.domain.model.Song

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
)
