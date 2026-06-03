package com.example.sona.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.PlaylistRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository,
    songRepository: SongRepository,
) : ViewModel() {
    private val selectedPlaylistId = MutableStateFlow<Long?>(null)
    private val newPlaylistName = MutableStateFlow("")

    private val playlistSongs = selectedPlaylistId.flatMapLatest { playlistId ->
        if (playlistId == null) {
            flowOf(emptyList())
        } else {
            playlistRepository.observePlaylistSongs(playlistId)
        }
    }

    val uiState = combine(
        playlistRepository.playlists,
        songRepository.songs,
        selectedPlaylistId,
        playlistSongs,
        newPlaylistName,
    ) { playlists, songs, selectedId, playlistSongs, name ->
        val selectedPlaylist = playlists.firstOrNull { it.id == selectedId }
        val playlistSongIds = playlistSongs.mapTo(mutableSetOf()) { it.id }

        PlaylistsUiState(
            playlists = playlists,
            selectedPlaylist = selectedPlaylist,
            playlistSongs = playlistSongs,
            availableSongs = songs.filterNot { it.id in playlistSongIds },
            newPlaylistName = name,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaylistsUiState(),
    )

    fun setNewPlaylistName(name: String) {
        newPlaylistName.value = name
    }

    fun createPlaylist() {
        val name = newPlaylistName.value.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            selectedPlaylistId.value = playlistId
            newPlaylistName.value = ""
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        selectedPlaylistId.value = playlist.id
    }

    fun clearSelectedPlaylist() {
        selectedPlaylistId.value = null
    }

    fun addSong(song: Song) {
        val playlistId = selectedPlaylistId.value ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun removeSong(song: Song) {
        val playlistId = selectedPlaylistId.value ?: return
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, song.id)
        }
    }
}

class PlaylistsViewModelFactory(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistsViewModel::class.java)) {
            return PlaylistsViewModel(playlistRepository, songRepository) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
