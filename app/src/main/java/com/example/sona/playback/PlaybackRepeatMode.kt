package com.example.sona.playback

enum class PlaybackRepeatMode {
    OFF,
    ALL,
    ONE,
}

fun PlaybackRepeatMode.next(): PlaybackRepeatMode = when (this) {
    PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
    PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
    PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
}
