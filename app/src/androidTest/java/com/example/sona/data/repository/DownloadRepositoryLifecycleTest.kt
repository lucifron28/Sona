package com.example.sona.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sona.data.database.SonaDatabase
import com.example.sona.domain.model.DownloadStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadRepositoryLifecycleTest {
    @Test
    fun cancellingPreventsCompletionAndRetryCanReuseTheRecord() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SonaDatabase::class.java,
        ).allowMainThreadQueries().build()
        val repository = DownloadRepository(database.downloadDao(), now = { 100L })

        try {
            val id = repository.enqueue("https://example.org/audio")
            assertTrue(repository.cancelDownload(id))
            assertEquals(DownloadStatus.CANCELLED, repository.getDownload(id)?.status)
            assertFalse(
                repository.markCompleted(
                    id = id,
                    title = "Late result",
                    outputUri = "file:///music/late.m4a",
                ),
            )
            assertEquals(DownloadStatus.CANCELLED, repository.getDownload(id)?.status)

            assertTrue(repository.retryDownload(id))
            assertEquals(DownloadStatus.QUEUED, repository.getDownload(id)?.status)
            assertTrue(
                repository.findImportCandidates("https://example.org/audio")
                    .any { it.id == id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun completedImportsAreRetainedForDuplicateDetectionAndHistoryRemoval() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SonaDatabase::class.java,
        ).allowMainThreadQueries().build()
        val repository = DownloadRepository(database.downloadDao(), now = { 100L })

        try {
            val id = repository.enqueue("https://example.org/audio")
            assertTrue(
                repository.markCompleted(
                    id = id,
                    title = "Finished track",
                    outputUri = "file:///music/finished.m4a",
                ),
            )
            assertTrue(
                repository.findImportCandidates("https://example.org/audio")
                    .any { it.id == id && it.status == DownloadStatus.COMPLETED },
            )
            assertTrue(repository.removeHistory(id))
            assertNull(repository.getDownload(id))
        } finally {
            database.close()
        }
    }
}
