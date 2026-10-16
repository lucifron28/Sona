package com.example.sona

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sona.data.settings.ThemePreference
import com.example.sona.ui.SonaApp
import com.example.sona.ui.theme.SonaTheme

class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl = intent.sharedUrl()
        val appContainer = (application as SonaApplication).appContainer
        setContent {
            val themePreference by appContainer.settingsRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)

            SonaTheme(themePreference = themePreference) {
                SonaApp(
                    appContainer = appContainer,
                    sharedUrl = sharedUrl,
                    onSharedUrlConsumed = { sharedUrl = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedUrl = intent.sharedUrl()
    }

    private fun Intent.sharedUrl(): String? {
        if (action != Intent.ACTION_SEND) return null
        val sharedText = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val candidate = SHARED_URL_REGEX.find(sharedText)
            ?.value
            ?.trimEnd('.', ',', ')', ']')
            ?: return null
        val scheme = Uri.parse(candidate).scheme?.lowercase()
        return candidate.takeIf { scheme == "http" || scheme == "https" }
    }

    private companion object {
        val SHARED_URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    }
}
