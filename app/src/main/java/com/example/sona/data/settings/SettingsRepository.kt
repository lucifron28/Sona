package com.example.sona.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    val themePreference: Flow<ThemePreference> = context.settingsDataStore.data
        .map { preferences ->
            preferences[THEME_PREFERENCE_KEY]?.toThemePreference() ?: ThemePreference.SYSTEM
        }

    suspend fun setThemePreference(themePreference: ThemePreference) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = themePreference.name
        }
    }

    private fun String.toThemePreference(): ThemePreference = runCatching {
        ThemePreference.valueOf(this)
    }.getOrDefault(ThemePreference.SYSTEM)

    private companion object {
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
    }
}
