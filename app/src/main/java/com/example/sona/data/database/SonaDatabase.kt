package com.example.sona.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sona.data.dao.SongDao
import com.example.sona.data.entities.SongEntity

@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SonaDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        fun create(context: Context): SonaDatabase = Room.databaseBuilder(
            context.applicationContext,
            SonaDatabase::class.java,
            "sona.db",
        ).build()
    }
}
