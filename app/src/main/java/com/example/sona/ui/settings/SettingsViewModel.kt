package com.example.sona.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sona.data.settings.SettingsRepository
import com.example.sona.data.settings.ThemePreference
import com.example.sona.downloader.UrlImportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val urlImportManager: UrlImportManager,
) : ViewModel() {
    private val downloaderUpdateState = MutableStateFlow(DownloaderUpdateState())

    val uiState = combine(
        settingsRepository.themePreference,
        downloaderUpdateState,
    ) { themePreference, updateState ->
        SettingsUiState(
            selectedTheme = themePreference,
            isUpdatingDownloader = updateState.isUpdating,
            downloaderMessage = updateState.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(themePreference)
        }
    }

    fun updateDownloader() {
        if (downloaderUpdateState.value.isUpdating) return
        downloaderUpdateState.value = DownloaderUpdateState(isUpdating = true)

        viewModelScope.launch {
            val message = try {
                urlImportManager.updateDownloader()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                error.message ?: "Downloader update failed."
            }
            downloaderUpdateState.value = DownloaderUpdateState(message = message)
        }
    }
}

private data class DownloaderUpdateState(
    val isUpdating: Boolean = false,
    val message: String? = null,
)

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val urlImportManager: UrlImportManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsRepository, urlImportManager) as T
        }
        error("Unknown ViewModel class ${modelClass.name}")
    }
}
