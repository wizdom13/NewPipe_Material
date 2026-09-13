package org.schabi.newpipe.settings.export

import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TakeoutParserTest {
    @Test
    fun zipCombinesPlaylistOrderAndHistoryWhileIgnoringOtherProducts() {
        val file = File.createTempFile("takeout-test", ".zip")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun entry(name: String, text: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray())
                    zip.closeEntry()
                }
                entry("Takeout/YouTube and YouTube Music/playlists/Science-videos.csv", "\uFEFFVideo ID,Time Added\r\nabcdefghijk,2024-01-01\r\nlmnopqrstuv,2024-01-02\r\n")
                entry("Takeout/YouTube and YouTube Music/history/watch-history.json", """[{"title":"Watched Science","titleUrl":"https://www.youtube.com/watch?v=abcdefghijk","time":"2024-02-03T04:05:06.123Z"},{"title":"Deleted video","time":"2024-02-03T00:00:00Z"}]""")
                entry("Takeout/My Activity/Search/search-history.json", "invalid ignored file")
            }
            val result = TakeoutParser.parse(file, "takeout.zip")
            assertEquals("Science", result.playlists.single().name)
            assertEquals(listOf("abcdefghijk", "lmnopqrstuv"), result.playlists.single().videos.map { it.title })
            assertEquals(Instant.parse("2024-02-03T04:05:06.123Z").toEpochMilli(), result.history.single().timestamp)
            assertEquals("Science", result.history.single().video.title)
            assertEquals(1, result.skipped)
        } finally {
            file.delete()
        }
    }

    @Test
    fun csvSupportsQuotedMetadataAndRejectsBrokenQuotes() {
        assertEquals(listOf(listOf("a,b", "two\nlines", "say \"hi\"")), TakeoutParser.csv("\"a,b\",\"two\nlines\",\"say \"\"hi\"\"\""))
        assertThrows(IllegalArgumentException::class.java) { TakeoutParser.csv("\"unclosed") }
    }

    @Test
    fun htmlPreservesUtcDatesAndSkipsUnknownLocalizedDates() {
        val file = File.createTempFile("watch-history", ".html")
        try {
            file.writeText("""<div class="content-cell">Watched <a href="https://www.youtube.com/watch?v=abcdefghijk">Science</a><br>Feb 3, 2024, 4:05:06 AM UTC</div>""")
            val result = TakeoutParser.parse(file, "watch-history.html")
            assertEquals(Instant.parse("2024-02-03T04:05:06Z").toEpochMilli(), result.history.single().timestamp)
        } finally {
            file.delete()
        }
    }
}
