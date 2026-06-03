package com.example.sona

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sona.data.settings.ThemePreference
import com.example.sona.ui.SonaApp
import com.example.sona.ui.theme.SonaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as SonaApplication).appContainer
        setContent {
            val themePreference by appContainer.settingsRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)

            SonaTheme(themePreference = themePreference) {
                SonaApp(appContainer = appContainer)
            }
        }
    }
}
