package com.example.sona.ui.library

import com.example.sona.domain.model.Song

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
)
