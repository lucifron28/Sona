package com.example.sona.di

import android.content.Context
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.DownloadRepository
import com.example.sona.data.repository.PlaylistRepository
import com.example.sona.data.repository.SongRepository
import com.example.sona.data.settings.SettingsRepository
import com.example.sona.playback.PlayerController
import com.example.sona.storage.AppMusicStorage

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    private val database: SonaDatabase by lazy {
        SonaDatabase.create(applicationContext)
    }

    val songRepository: SongRepository by lazy {
        SongRepository(database.songDao())
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(database.playlistDao())
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(database.downloadDao())
    }

    val appMusicStorage: AppMusicStorage by lazy {
        AppMusicStorage(applicationContext)
    }

    val playerController: PlayerController by lazy {
        PlayerController(applicationContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }
}
