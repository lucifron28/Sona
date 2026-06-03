package com.example.sona

import android.app.Application
import com.example.sona.di.AppContainer
import com.example.sona.downloader.DownloaderAutoUpdateScheduler

class SonaApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        DownloaderAutoUpdateScheduler.schedule(this)
    }
}
