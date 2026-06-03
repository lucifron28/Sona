package com.example.sona.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.PlaylistRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song
import com.example.sona.storage.AppMusicStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val appMusicStorage: AppMusicStorage,
) : ViewModel() {
    private val importState = MutableStateFlow(ImportState())
    private val selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val selectedView = MutableStateFlow(LibraryView.SONGS)
    private val searchQuery = MutableStateFlow("")
    private val editFormState = MutableStateFlow<EditFormState?>(null)
    private val trackActionsSong = MutableStateFlow<Song?>(null)
    private val selectedGroupKey = MutableStateFlow<LibraryGroupKey?>(null)
    private val libraryControls = combine(
        selectedFilter,
        selectedView,
        searchQuery,
        editFormState,
        selectedGroupKey,
    ) { filter, view, query, editForm, groupKey ->
        LibraryControlState(
            filter = filter,
            view = view,
            query = query,
            editForm = editForm,
            groupKey = groupKey,
        )
    }

    val uiState = combine(
        songRepository.songs,
        playlistRepository.playlists,
        importState,
        libraryControls,
        trackActionsSong,
    ) { songs, playlists, importState, controls, actionSong ->
        val filteredSongs = searchSongsForLibrary(
            songs = filterSongsForLibrary(songs, controls.filter),
            query = controls.query,
        )
        val artistGroups = artistGroupsForLibrary(filteredSongs)
        val albumGroups = albumGroupsForLibrary(filteredSongs)
        val selectedGroup = controls.groupKey
            ?.takeIf { it.view == controls.view }
            ?.let { key ->
                val groups = when (key.view) {
                    LibraryView.ARTISTS -> artistGroups
                    LibraryView.ALBUMS -> albumGroups
                    LibraryView.SONGS -> emptyList()
                }
                groups.firstOrNull { group -> group.name.equals(key.name, ignoreCase = true) }
            }
            ?.let { group ->
                SelectedLibraryGroup(
                    view = controls.view,
                    group = group,
                )
            }
        val currentActionSong = actionSong?.let { selected ->
            songs.firstOrNull { it.id == selected.id } ?: selected
        }

        LibraryUiState(
            songs = filteredSongs,
            artistGroups = artistGroups,
            albumGroups = albumGroups,
            selectedFilter = controls.filter,
            selectedView = controls.view,
            searchQuery = controls.query,
            isImporting = importState.isImporting,
            errorMessage = importState.errorMessage,
            selectedGroup = selectedGroup,
            editState = controls.editForm?.toTrackEditState(songs),
            trackActionsState = currentActionSong?.let { song ->
                TrackActionsState(
                    song = song,
                    playlists = playlists,
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            importState.update { it.copy(isImporting = true, errorMessage = null) }

            runCatching {
                val song = appMusicStorage.importAudio(uri)
                songRepository.addSong(song)
            }.onFailure { error ->
                importState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = error.message ?: "Import failed.",
                    )
                }
            }.onSuccess {
                importState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun clearError() {
        importState.update { it.copy(errorMessage = null) }
    }

    fun setFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
        selectedGroupKey.value = null
    }

    fun setView(view: LibraryView) {
        selectedView.value = view
        selectedGroupKey.value = null
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
        selectedGroupKey.value = null
    }

    fun selectGroup(view: LibraryView, group: LibraryGroup) {
        if (view == LibraryView.SONGS) return
        selectedGroupKey.value = LibraryGroupKey(
            view = view,
            name = group.name,
        )
    }

    fun clearSelectedGroup() {
        selectedGroupKey.value = null
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun startEditing(song: Song) {
        trackActionsSong.value = null
        editFormState.value = EditFormState(
            song = song,
            title = song.title,
            artist = song.artist,
        )
    }

    fun updateEditTitle(title: String) {
        editFormState.update { editForm ->
            editForm?.copy(title = title)
        }
    }

    fun updateEditArtist(artist: String) {
        editFormState.update { editForm ->
            editForm?.copy(artist = artist)
        }
    }

    fun dismissEditor() {
        editFormState.value = null
    }

    fun showTrackActions(song: Song) {
        trackActionsSong.value = song
    }

    fun dismissTrackActions() {
        trackActionsSong.value = null
    }

    fun renameFromTrackActions(song: Song) {
        trackActionsSong.value = null
        startEditing(song)
    }

    fun addTrackActionSongToPlaylist(playlist: Playlist) {
        val song = trackActionsSong.value ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlist.id, song.id)
            trackActionsSong.value = null
        }
    }

    fun saveEditedTrack() {
        val editForm = editFormState.value ?: return
        val title = editForm.title.trim()
        val artist = editForm.artist.trim()
        if (title.isBlank() || artist.isBlank()) return

        viewModelScope.launch {
            songRepository.updateSong(
                editForm.song.copy(
                    title = title,
                    artist = artist,
                ),
            )
            editFormState.value = null
        }
    }
}

private data class ImportState(
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
)

private data class LibraryControlState(
    val filter: LibraryFilter,
    val view: LibraryView,
    val query: String,
    val editForm: EditFormState?,
    val groupKey: LibraryGroupKey?,
)

private data class LibraryGroupKey(
    val view: LibraryView,
    val name: String,
)

private data class EditFormState(
    val song: Song,
    val title: String,
    val artist: String,
) {
    fun toTrackEditState(songs: List<Song>): TrackEditState {
        val artistQuery = artist.trim()
        val suggestions = songs
            .asSequence()
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter { suggestion ->
                suggestion.equals(artistQuery, ignoreCase = true).not() &&
                    (artistQuery.isBlank() || suggestion.contains(artistQuery, ignoreCase = true))
            }
            .sorted()
            .take(6)
            .toList()

        return TrackEditState(
            song = song,
            title = title,
            artist = artist,
            artistSuggestions = suggestions,
        )
    }
}

class LibraryViewModelFactory(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val appMusicStorage: AppMusicStorage,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(songRepository, playlistRepository, appMusicStorage) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
