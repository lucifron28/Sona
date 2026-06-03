package com.example.sona

import android.app.Application
import com.example.sona.di.AppContainer

class SonaApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
