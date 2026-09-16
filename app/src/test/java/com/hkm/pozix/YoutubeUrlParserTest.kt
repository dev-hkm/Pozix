package com.hkm.pozix

import com.hkm.pozix.util.YoutubeUrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class YoutubeUrlParserTest {
    @Test
    fun acceptsWatchUrlWithTrailingSlash() {
        assertEquals(
            "dQw4w9WgXcQ",
            YoutubeUrlParser.parse("https://www.youtube.com/watch/?v=dQw4w9WgXcQ")?.videoId
        )
    }

    @Test
    fun acceptsLegacyVPath() {
        assertNotNull(YoutubeUrlParser.parse("https://youtube.com/v/dQw4w9WgXcQ"))
    }
}
