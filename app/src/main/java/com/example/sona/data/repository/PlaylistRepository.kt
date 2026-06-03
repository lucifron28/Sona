package com.example.sona.data.repository

import com.example.sona.data.dao.PlaylistDao
import com.example.sona.data.entities.PlaylistEntity
import com.example.sona.data.entities.PlaylistSongCrossRef
import com.example.sona.data.entities.toPlaylist
import com.example.sona.data.entities.toSong
import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    val playlists: Flow<List<Playlist>> = playlistDao.observePlaylists()
        .map { playlists -> playlists.map { it.toPlaylist() } }

    fun observePlaylistSongs(playlistId: Long): Flow<List<Song>> =
        playlistDao.observePlaylistSongs(playlistId)
            .map { songs -> songs.map { it.toSong() } }

    suspend fun createPlaylist(name: String): Long {
        val timestamp = now()
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val nextPosition = playlistDao.maxPosition(playlistId) + 1
        playlistDao.insertPlaylistSong(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition,
            ),
        )
        playlistDao.touchPlaylist(playlistId, now())
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        playlistDao.touchPlaylist(playlistId, now())
    }
}
