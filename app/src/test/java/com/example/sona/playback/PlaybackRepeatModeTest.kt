package com.example.sona.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRepeatModeTest {
    @Test
    fun next_cyclesThroughRepeatModes() {
        assertEquals(PlaybackRepeatMode.ALL, PlaybackRepeatMode.OFF.next())
        assertEquals(PlaybackRepeatMode.ONE, PlaybackRepeatMode.ALL.next())
        assertEquals(PlaybackRepeatMode.OFF, PlaybackRepeatMode.ONE.next())
    }
}
