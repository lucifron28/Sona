package com.example.sona.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.SongRepository
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
    private val appMusicStorage: AppMusicStorage,
) : ViewModel() {
    private val importState = MutableStateFlow(ImportState())
    private val selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val editFormState = MutableStateFlow<EditFormState?>(null)

    val uiState = combine(
        songRepository.songs,
        importState,
        selectedFilter,
        editFormState,
    ) { songs, importState, filter, editForm ->
        LibraryUiState(
            songs = filterSongsForLibrary(songs, filter),
            selectedFilter = filter,
            isImporting = importState.isImporting,
            errorMessage = importState.errorMessage,
            editState = editForm?.toTrackEditState(songs),
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
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun startEditing(song: Song) {
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
    private val appMusicStorage: AppMusicStorage,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(songRepository, appMusicStorage) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
