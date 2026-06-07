package com.example.sona.downloader

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadUrlNormalizerTest {
    @Test
    fun stripsRadioPlaylistParametersFromWatchUrls() {
        val url = "https://www.youtube.com/watch?v=RAov563V8vI&list=RDRAov563V8vI&start_radio=1"

        assertEquals(
            "https://www.youtube.com/watch?v=RAov563V8vI",
            url.normalizedDownloadUrl(),
        )
    }

    @Test
    fun normalizesShortUrls() {
        val url = "https://youtu.be/RAov563V8vI?si=abc123"

        assertEquals(
            "https://www.youtube.com/watch?v=RAov563V8vI",
            url.normalizedDownloadUrl(),
        )
    }

    @Test
    fun normalizesShortsUrls() {
        val url = "https://m.youtube.com/shorts/RAov563V8vI?feature=share"

        assertEquals(
            "https://www.youtube.com/watch?v=RAov563V8vI",
            url.normalizedDownloadUrl(),
        )
    }

    @Test
    fun leavesNonYoutubeUrlsAlone() {
        val url = "https://example.com/watch?v=RAov563V8vI&list=abc"

        assertEquals(url, url.normalizedDownloadUrl())
    }
}
