package com.example.sona.ui.playlists

import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val playlistSongs: List<Song> = emptyList(),
    val availableSongs: List<Song> = emptyList(),
    val newPlaylistName: String = "",
)
