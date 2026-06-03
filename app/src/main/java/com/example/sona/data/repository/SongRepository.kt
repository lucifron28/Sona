package com.example.sona.data.repository

import com.example.sona.data.dao.SongDao
import com.example.sona.data.entities.toEntity
import com.example.sona.data.entities.toSong
import com.example.sona.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepository(
    private val songDao: SongDao,
) {
    val songs: Flow<List<Song>> = songDao.observeSongs()
        .map { entities -> entities.map { it.toSong() } }

    suspend fun getSong(id: Long): Song? = songDao.getSong(id)?.toSong()

    suspend fun addSong(song: Song): Long = songDao.insertSong(song.toEntity())

    suspend fun updateSong(song: Song) {
        songDao.updateSong(song.toEntity())
    }

    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song.toEntity())
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        songDao.setFavorite(id, isFavorite)
    }

    suspend fun recordPlayed(id: Long, playedAt: Long) {
        songDao.recordPlayed(id, playedAt)
    }
}
