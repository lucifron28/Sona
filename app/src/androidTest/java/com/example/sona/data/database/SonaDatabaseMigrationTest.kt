package com.example.sona.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SonaDatabaseMigrationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun upgradesVersion1ToLatestSchema() = runBlocking {
        assertUpgradedFrom(1)
    }

    @Test
    fun upgradesVersion2ToLatestSchema() = runBlocking {
        assertUpgradedFrom(2)
    }

    @Test
    fun upgradesVersion3ToLatestSchema() = runBlocking {
        assertUpgradedFrom(3)
    }

    private suspend fun assertUpgradedFrom(version: Int) {
        val databaseName = "sona-migration-v$version.db"
        context.deleteDatabase(databaseName)
        createLegacyDatabase(context.getDatabasePath(databaseName), version)

        val database = Room.databaseBuilder(
            context,
            SonaDatabase::class.java,
            databaseName,
        )
            .allowMainThreadQueries()
            .addMigrations(*SonaDatabase.ALL_MIGRATIONS)
            .build()

        try {
            val song = database.songDao().getSong(1L)
            assertNotNull(song)
            assertEquals("Legacy track", song?.title)

            val downloadColumns = mutableSetOf<String>()
            database.openHelper.writableDatabase.query("PRAGMA table_info(download_items)").use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) downloadColumns += cursor.getString(nameColumn)
            }
            assertEquals(
                setOf(
                    "id",
                    "url",
                    "title",
                    "status",
                    "progress",
                    "workId",
                    "outputUri",
                    "errorMessage",
                    "diagnosticMessage",
                    "createdAt",
                    "completedAt",
                ),
                downloadColumns,
            )

            val legacyDownload = database.downloadDao().getDownload(1L)
            if (version == 3) {
                assertNotNull(legacyDownload)
                assertEquals("https://example.org/audio", legacyDownload?.url)
                assertNull(legacyDownload?.workId)
                assertNull(legacyDownload?.diagnosticMessage)
            } else {
                assertNull(legacyDownload)
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createLegacyDatabase(file: java.io.File, version: Int) {
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE songs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT,
                    durationMs INTEGER NOT NULL,
                    uri TEXT NOT NULL,
                    artworkUri TEXT,
                    dateAdded INTEGER NOT NULL,
                    sourceType TEXT NOT NULL,
                    sourceUrl TEXT,
                    playCount INTEGER NOT NULL,
                    lastPlayedAt INTEGER,
                    isFavorite INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE UNIQUE INDEX index_songs_uri ON songs(uri)")
            database.execSQL(
                """
                INSERT INTO songs (
                    id, title, artist, album, durationMs, uri, artworkUri, dateAdded,
                    sourceType, sourceUrl, playCount, lastPlayedAt, isFavorite
                ) VALUES (1, 'Legacy track', 'Legacy artist', NULL, 180000,
                    'file:///legacy.m4a', NULL, 123, 'LOCAL_FILE', NULL, 2, 111, 0)
                """.trimIndent(),
            )

            if (version >= 2) createLegacyPlaylistTables(database)
            if (version >= 3) createLegacyDownloadTable(database)
            database.execSQL("PRAGMA user_version = $version")
        }
    }

    private fun createLegacyPlaylistTables(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE playlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE playlist_songs (
                playlistId INTEGER NOT NULL,
                songId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY(playlistId, songId),
                FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(songId) REFERENCES songs(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_playlist_songs_playlistId ON playlist_songs(playlistId)")
        database.execSQL("CREATE INDEX index_playlist_songs_songId ON playlist_songs(songId)")
        database.execSQL(
            "INSERT INTO playlists(id, name, createdAt, updatedAt) VALUES (1, 'Legacy', 123, 123)",
        )
        database.execSQL(
            "INSERT INTO playlist_songs(playlistId, songId, position) VALUES (1, 1, 0)",
        )
    }

    private fun createLegacyDownloadTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE download_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                url TEXT NOT NULL,
                title TEXT,
                status TEXT NOT NULL,
                progress REAL NOT NULL,
                outputUri TEXT,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO download_items(id, url, title, status, progress, createdAt)
            VALUES (1, 'https://example.org/audio', 'Legacy import', 'FAILED', 12.0, 456)
            """.trimIndent(),
        )
    }
}
