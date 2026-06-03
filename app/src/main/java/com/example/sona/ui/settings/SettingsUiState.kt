package com.example.sona.ui.settings

import com.example.sona.data.settings.ThemePreference

data class SettingsUiState(
    val selectedTheme: ThemePreference = ThemePreference.SYSTEM,
)
