package com.example.sona.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {
    @Test
    fun formatDuration_formatsZeroDuration() {
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun formatDuration_formatsMinuteDurations() {
        assertEquals("1:05", formatDuration(65_000L))
    }

    @Test
    fun formatDuration_formatsHourDurations() {
        assertEquals("1:02:06", formatDuration(3_726_000L))
    }

    @Test
    fun formatDuration_clampsNegativeDurations() {
        assertEquals("0:00", formatDuration(-1_000L))
    }
}
