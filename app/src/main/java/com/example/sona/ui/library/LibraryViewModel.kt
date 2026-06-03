package com.example.sona.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.SongRepository
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

    val uiState = combine(songRepository.songs, importState) { songs, importState ->
        LibraryUiState(
            songs = songs,
            isImporting = importState.isImporting,
            errorMessage = importState.errorMessage,
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
}

private data class ImportState(
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
)

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
