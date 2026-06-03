package com.example.sona.playback

import com.example.sona.domain.model.Song

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = -1,
    val queueSize: Int = 0,
    val errorMessage: String? = null,
)
