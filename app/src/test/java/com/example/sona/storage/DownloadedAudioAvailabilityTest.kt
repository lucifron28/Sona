package com.example.sona.storage

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadedAudioAvailabilityTest {
    @Test
    fun onlyNonEmptyFilesInsideMusicStorageAreAvailable() {
        val temporaryDirectory = Files.createTempDirectory("sona-audio-availability").toFile()
        try {
            val musicDirectory = File(temporaryDirectory, "music").apply { mkdirs() }
            val audioFile = File(musicDirectory, "song.m4a").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
            val emptyFile = File(musicDirectory, "empty.m4a").apply { createNewFile() }
            val outsideFile = File(temporaryDirectory, "outside.m4a").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
            val missingFile = File(musicDirectory, "missing.m4a")

            assertTrue(audioFile.isAvailableStoredAudioIn(musicDirectory))
            assertFalse(emptyFile.isAvailableStoredAudioIn(musicDirectory))
            assertFalse(outsideFile.isAvailableStoredAudioIn(musicDirectory))
            assertFalse(missingFile.isAvailableStoredAudioIn(musicDirectory))
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }
}
