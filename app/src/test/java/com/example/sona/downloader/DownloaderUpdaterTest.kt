package com.example.sona.downloader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderUpdaterTest {
    @Test
    fun isUpdateStale_returnsTrueWhenNeverUpdated() {
        assertTrue(
            isUpdateStale(
                lastUpdateAt = 0L,
                now = 100L,
                intervalMs = 50L,
            ),
        )
    }

    @Test
    fun isUpdateStale_returnsFalseInsideInterval() {
        assertFalse(
            isUpdateStale(
                lastUpdateAt = 100L,
                now = 120L,
                intervalMs = 50L,
            ),
        )
    }

    @Test
    fun isUpdateStale_returnsTrueAtIntervalBoundary() {
        assertTrue(
            isUpdateStale(
                lastUpdateAt = 100L,
                now = 150L,
                intervalMs = 50L,
            ),
        )
    }
}
