package com.example.sona.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.downloader.UrlImportManager
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.playback.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val urlImportManager: UrlImportManager,
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val formState = MutableStateFlow(DownloadFormState())
    private val snackbarEvent = MutableStateFlow<DownloadSnackbarEvent?>(null)
    private val knownStatuses = mutableMapOf<Long, DownloadStatus>()

    init {
        viewModelScope.launch {
            downloadRepository.downloads
                .onEach { downloads ->
                    downloads.forEach { download ->
                        val previousStatus = knownStatuses.put(download.id, download.status)
                        if (
                            previousStatus != null &&
                            previousStatus != download.status &&
                            download.status.isTerminal
                        ) {
                            snackbarEvent.value = download.toSnackbarEvent()
                        }
                    }
                    val currentIds = downloads.mapTo(mutableSetOf()) { it.id }
                    knownStatuses.keys.retainAll(currentIds)
                }
                .collect {}
        }
    }

    val uiState = combine(downloadRepository.downloads, formState, snackbarEvent) { downloads, form, event ->
        DownloadsUiState(
            downloads = downloads,
            url = form.url,
            message = form.message,
            snackbarEvent = event,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState(),
    )

    fun setUrl(url: String) {
        formState.value = formState.value.copy(url = url, message = null)
    }

    fun enqueueImport() {
        val url = formState.value.url.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            runCatching {
                urlImportManager.enqueue(url)
            }.onSuccess {
                formState.value = formState.value.copy(
                    url = "",
                    message = "Import request accepted. Active or completed duplicates reuse the existing record.",
                )
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    message = error.message ?: "Unable to queue import.",
                )
            }
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            runCatching {
                urlImportManager.cancel(id)
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    message = error.message ?: "Unable to cancel this download.",
                )
            }
        }
    }

    fun retryDownload(id: Long) {
        viewModelScope.launch {
            runCatching {
                urlImportManager.retry(id)
            }.onSuccess { queued ->
                formState.value = formState.value.copy(
                    message = if (queued) "Retry queued." else "This import is no longer eligible for retry.",
                )
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    message = error.message ?: "Unable to retry this import.",
                )
            }
        }
    }

    fun removeHistory(id: Long) {
        viewModelScope.launch {
            runCatching {
                urlImportManager.removeHistory(id)
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    message = error.message ?: "Unable to remove this history item.",
                )
            }
        }
    }

    fun playDownload(id: Long) {
        viewModelScope.launch {
            val download = downloadRepository.getDownload(id)
            val outputUri = download?.outputUri
            val song = if (outputUri == null) {
                null
            } else {
                songRepository.getSongByUri(outputUri)
            }
            if (song == null) {
                formState.value = formState.value.copy(
                    message = "This audio file is no longer in your library.",
                )
                return@launch
            }
            playerController.play(song, listOf(song))
        }
    }

    fun consumeSnackbar() {
        snackbarEvent.value = null
    }
}

private data class DownloadFormState(
    val url: String = "",
    val message: String? = null,
)

private val DownloadStatus.isTerminal: Boolean
    get() = when (this) {
        DownloadStatus.COMPLETED,
        DownloadStatus.FAILED,
        DownloadStatus.CANCELLED,
        -> true
        DownloadStatus.QUEUED,
        DownloadStatus.FETCHING_METADATA,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.EXTRACTING,
        -> false
    }

class DownloadsViewModelFactory(
    private val downloadRepository: DownloadRepository,
    private val urlImportManager: UrlImportManager,
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
            return DownloadsViewModel(
                downloadRepository,
                urlImportManager,
                songRepository,
                playerController,
            ) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
