package com.example.sona.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sona.data.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSong(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query(
        """
        UPDATE songs
        SET playCount = playCount + 1,
            lastPlayedAt = :playedAt
        WHERE id = :id
        """,
    )
    suspend fun recordPlayed(id: Long, playedAt: Long)
}
