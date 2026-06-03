package com.example.sona.di

import android.content.Context
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.SongRepository
import com.example.sona.storage.AppMusicStorage

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    private val database: SonaDatabase by lazy {
        SonaDatabase.create(applicationContext)
    }

    val songRepository: SongRepository by lazy {
        SongRepository(database.songDao())
    }

    val appMusicStorage: AppMusicStorage by lazy {
        AppMusicStorage(applicationContext)
    }
}
