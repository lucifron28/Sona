package com.example.sona.downloader

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderFailureTest {
    @Test
    fun shouldRepairYoutube403_matchesYoutubeForbiddenOutput() {
        val error = RuntimeException("")
        val line = "ERROR: unable to download video data: HTTP Error 403: Forbidden"

        assertTrue(error.shouldRepairYoutube403("https://youtu.be/G39G7eoUClE", line))
    }

    @Test
    fun shouldRepairYoutube403_ignoresNonYoutubeUrl() {
        val error = RuntimeException("HTTP Error 403: Forbidden")

        assertFalse(error.shouldRepairYoutube403("https://example.com/audio.mp3", null))
    }

    @Test
    fun userVisibleDownloadMessage_usesLastDownloaderLineWhenExceptionMessageIsBlank() {
        val error = RuntimeException("")

        assertEquals(
            "Downloader stopped after: ERROR: unable to download video data: HTTP Error 403: Forbidden",
            error.userVisibleDownloadMessage("ERROR: unable to download video data: HTTP Error 403: Forbidden"),
        )
    }

    @Test
    fun cleanedDownloaderLine_trimsAndCompactsWhitespace() {
        assertEquals(
            "[download] Sleeping 6.00 seconds as required by the site...",
            "  [download]   Sleeping 6.00 seconds   as required by the site...  ".cleanedDownloaderLine(),
        )
    }
    @Test
    fun shouldRetryDownload_retriesTransientNetworkErrorsWithinLimit() {
        assertTrue(
            IOException("connection reset").shouldRetryDownload(
                lastDownloaderLine = null,
                runAttemptCount = 2,
            ),
        )
        assertTrue(
            RuntimeException("HTTP Error 503: Service Unavailable").shouldRetryDownload(
                lastDownloaderLine = null,
                runAttemptCount = 0,
            ),
        )
    }

    @Test
    fun shouldRetryDownload_rejectsPermanentErrorsAndExhaustedAttempts() {
        assertFalse(
            RuntimeException("HTTP Error 404: Not Found").shouldRetryDownload(
                lastDownloaderLine = null,
                runAttemptCount = 0,
            ),
        )
        assertFalse(
            IOException("timeout").shouldRetryDownload(
                lastDownloaderLine = null,
                runAttemptCount = MAX_DOWNLOAD_RETRIES,
            ),
        )
    }
}
