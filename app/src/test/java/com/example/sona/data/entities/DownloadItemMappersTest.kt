package com.example.sona.data.entities

import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadItemMappersTest {
    @Test
    fun toDownloadItem_mapsEntityFields() {
        val entity = DownloadItemEntity(
            id = 2L,
            url = "https://example.com/audio",
            title = "Imported track",
            status = DownloadStatus.DOWNLOADING.name,
            progress = 42f,
            workId = "work-123",
            outputUri = null,
            errorMessage = null,
            diagnosticMessage = "Downloading 42%",
            createdAt = 100L,
            completedAt = null,
        )

        val item = entity.toDownloadItem()

        assertEquals(2L, item.id)
        assertEquals("https://example.com/audio", item.url)
        assertEquals("Imported track", item.title)
        assertEquals(DownloadStatus.DOWNLOADING, item.status)
        assertEquals(42f, item.progress)
        assertEquals("work-123", item.workId)
        assertNull(item.outputUri)
        assertNull(item.errorMessage)
        assertEquals("Downloading 42%", item.diagnosticMessage)
        assertEquals(100L, item.createdAt)
        assertNull(item.completedAt)
    }

    @Test
    fun toDownloadItem_defaultsUnknownStatusToFailed() {
        val entity = DownloadItemEntity(
            url = "https://example.com/audio",
            status = "UNKNOWN",
            createdAt = 100L,
        )

        assertEquals(DownloadStatus.FAILED, entity.toDownloadItem().status)
    }

    @Test
    fun toEntity_mapsDownloadFields() {
        val item = DownloadItem(
            id = 5L,
            url = "https://example.com/audio",
            title = "Done",
            status = DownloadStatus.COMPLETED,
            progress = 100f,
            workId = "work-456",
            outputUri = "file:///audio/done.m4a",
            errorMessage = null,
            diagnosticMessage = "Complete",
            createdAt = 200L,
            completedAt = 300L,
        )

        val entity = item.toEntity()

        assertEquals(5L, entity.id)
        assertEquals("https://example.com/audio", entity.url)
        assertEquals("Done", entity.title)
        assertEquals(DownloadStatus.COMPLETED.name, entity.status)
        assertEquals(100f, entity.progress)
        assertEquals("work-456", entity.workId)
        assertEquals("file:///audio/done.m4a", entity.outputUri)
        assertNull(entity.errorMessage)
        assertEquals("Complete", entity.diagnosticMessage)
        assertEquals(200L, entity.createdAt)
        assertEquals(300L, entity.completedAt)
    }
}
