package com.example.sona

import android.app.Application
import androidx.work.Configuration
import com.example.sona.di.AppContainer
import com.example.sona.downloader.DownloaderAutoUpdateScheduler
import com.example.sona.downloader.UrlImportWorkerFactory

class SonaApplication : Application(), Configuration.Provider {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(UrlImportWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        DownloaderAutoUpdateScheduler.schedule(this)
    }
}
