package com.example.sona.downloader

import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus
import com.example.sona.domain.model.Song
import com.example.sona.domain.model.SourceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadImportDeduplicationTest {
    @Test
    fun completedImportWithoutAvailableAudioDoesNotSuppressReimport() = runBlocking {
        val completed = completedImport()

        assertNull(listOf(completed).findReusableImport { false })
    }

    @Test
    fun completedImportRequiresTheMatchingLibrarySongAndStoredAudioFile() {
        val completed = completedImport()
        val song = Song(
            id = 1L,
            title = "Downloaded track",
            artist = "Artist",
            durationMs = 1000L,
            uri = requireNotNull(completed.outputUri),
            dateAdded = 1L,
            sourceType = SourceType.IMPORTED_URL,
            sourceUrl = completed.url,
        )

        assertTrue(completed.hasAvailableLibraryAudio(song, outputFileAvailable = true))
        assertFalse(completed.hasAvailableLibraryAudio(null, outputFileAvailable = true))
        assertFalse(completed.hasAvailableLibraryAudio(song, outputFileAvailable = false))
        assertFalse(
            completed.hasAvailableLibraryAudio(
                song.copy(sourceUrl = "https://example.org/different"),
                outputFileAvailable = true,
            ),
        )
    }

    @Test
    fun completedImportWithAvailableAudioStillSuppressesDuplicate() = runBlocking {
        val completed = completedImport()

        assertEquals(completed, listOf(completed).findReusableImport { true })
    }

    @Test
    fun activeImportIsReusedWithoutCheckingCompletedAudio() = runBlocking {
        val active = DownloadItem(
            id = 2L,
            url = "https://example.org/audio",
            status = DownloadStatus.DOWNLOADING,
            createdAt = 2L,
        )
        var checkedAudio = false

        assertEquals(active, listOf(active).findReusableImport {
            checkedAudio = true
            false
        })
        assertFalse(checkedAudio)
    }

    private fun completedImport() = DownloadItem(
        id = 1L,
        url = "https://example.org/audio",
        status = DownloadStatus.COMPLETED,
        outputUri = "file:///music/sona-import-1.m4a",
        createdAt = 1L,
    )
}
