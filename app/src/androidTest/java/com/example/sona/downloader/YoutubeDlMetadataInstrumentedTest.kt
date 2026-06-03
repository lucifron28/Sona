package com.example.sona.downloader

import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeDlMetadataInstrumentedTest {
    @Test
    fun resolvesProvidedYoutubeUrlAsSingleVideo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val request = YoutubeDLRequest(TEST_URL).apply {
            addOption("--no-playlist")
        }

        YoutubeDL.getInstance().init(context)
        val videoInfo = YoutubeDL.getInstance().getInfo(request)

        assertEquals("RAov563V8vI", videoInfo.id)
        assertTrue(videoInfo.title.isNullOrBlank().not() || videoInfo.fulltitle.isNullOrBlank().not())
    }

    private companion object {
        const val TEST_URL =
            "https://www.youtube.com/watch?v=RAov563V8vI&list=RDRAov563V8vI&start_radio=1"
    }
}
