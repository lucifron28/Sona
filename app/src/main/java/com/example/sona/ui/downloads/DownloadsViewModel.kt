package com.example.sona.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.downloader.UrlImportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val urlImportManager: UrlImportManager,
) : ViewModel() {
    private val formState = MutableStateFlow(DownloadFormState())

    val uiState = combine(downloadRepository.downloads, formState) { downloads, form ->
        DownloadsUiState(
            downloads = downloads,
            url = form.url,
            isUpdatingDownloader = form.isUpdatingDownloader,
            message = form.message,
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
                    message = "Import queued.",
                )
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    message = error.message ?: "Unable to queue import.",
                )
            }
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }

    fun updateDownloader() {
        viewModelScope.launch {
            formState.value = formState.value.copy(
                isUpdatingDownloader = true,
                message = null,
            )
            runCatching {
                urlImportManager.updateDownloader()
            }.onSuccess { message ->
                formState.value = formState.value.copy(
                    isUpdatingDownloader = false,
                    message = message,
                )
            }.onFailure { error ->
                formState.value = formState.value.copy(
                    isUpdatingDownloader = false,
                    message = error.message ?: "Downloader update failed.",
                )
            }
        }
    }
}

private data class DownloadFormState(
    val url: String = "",
    val isUpdatingDownloader: Boolean = false,
    val message: String? = null,
)

class DownloadsViewModelFactory(
    private val downloadRepository: DownloadRepository,
    private val urlImportManager: UrlImportManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
            return DownloadsViewModel(downloadRepository, urlImportManager) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
