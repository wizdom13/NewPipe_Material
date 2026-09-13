package org.schabi.newpipe.settings.export

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipFile
import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory

internal data class TakeoutVideo(val url: String, val title: String)
internal data class TakeoutWatch(val video: TakeoutVideo, val timestamp: Long)
internal data class TakeoutPlaylist(val name: String, val videos: List<TakeoutVideo>)
internal data class TakeoutData(val playlists: List<TakeoutPlaylist>, val history: List<TakeoutWatch>, val skipped: Int)

internal object TakeoutParser {
    private const val MAX_FILE = 32 * 1024 * 1024
    private const val MAX_TOTAL = 64 * 1024 * 1024
    private const val MAX_RECORDS = 100000

    fun parse(file: File, name: String): TakeoutData {
        val playlists = mutableListOf<TakeoutPlaylist>()
        val history = mutableListOf<TakeoutWatch>()
        var skipped = 0
        var bytes = 0
        fun read(name: String, input: InputStream) {
            val data = input.readTakeoutBytes(MAX_FILE)
            require(data.size <= MAX_FILE) { "Takeout file exceeds 32 MiB; select a smaller export." }
            bytes += data.size
            require(bytes <= MAX_TOTAL) { "Selected Takeout data exceeds 64 MiB." }
            val text = data.toString(Charsets.UTF_8).removePrefix("\uFEFF")
            when (name.substringAfterLast('.').lowercase(Locale.ROOT)) {
                "csv" -> {
                    val rows = csv(text)
                    val headerIndex = rows.indexOfFirst { row -> row.any { it.trim().equals("Video ID", true) || it.trim().equals("Video Id", true) } }
                    if (headerIndex < 0) return
                    val header = rows[headerIndex].map { it.trim().lowercase(Locale.ROOT) }
                    val idColumn = header.indexOf("video id")
                    val titleColumn = header.indexOf("title")
                    val videos = rows.drop(headerIndex + 1).filter { it.any(String::isNotBlank) }.mapNotNull { row ->
                        video(row.getOrNull(idColumn).orEmpty(), row.getOrNull(titleColumn).orEmpty()).also { if (it == null) skipped++ }
                    }
                    val titleHeader = rows.take(headerIndex).indexOfFirst { row -> row.any { it.equals("Playlist Title", true) || it.equals("Title", true) } }
                    val playlistName = if (titleHeader >= 0) {
                        val column = rows[titleHeader].indexOfFirst { it.equals("Playlist Title", true) || it.equals("Title", true) }
                        rows.getOrNull(titleHeader + 1)?.getOrNull(column)
                    } else {
                        null
                    }
                    playlists.add(TakeoutPlaylist(playlistName?.takeIf { it.isNotBlank() } ?: name.substringAfterLast('/').removeSuffix(".csv").removeSuffix("-videos"), videos))
                }
                "json" -> {
                    for (entry in JsonParser.array().from(text)) {
                        val row = entry as? JsonObject ?: continue
                        val video = video(row.getString("titleUrl", ""), row.getString("title", "").removePrefix("Watched "))
                        val time = runCatching { Instant.parse(row.getString("time", "")).toEpochMilli() }.getOrNull()
                        if (video != null && time != null) history.add(TakeoutWatch(video, time)) else skipped++
                    }
                }
                "html" -> {
                    for (cell in Jsoup.parse(text).select("div.content-cell")) {
                        val link = cell.select("a[href]").firstOrNull { video(it.attr("href"), "") != null } ?: continue
                        val video = video(link.attr("href"), link.text()) ?: continue
                        val time = cell.wholeText().lines().mapNotNull(::htmlTime).firstOrNull()
                        if (time != null) history.add(TakeoutWatch(video, time)) else skipped++
                    }
                }
            }
            require(history.size + playlists.sumOf { it.videos.size } <= MAX_RECORDS) { "Takeout export exceeds 100,000 records." }
        }
        val zip = file.inputStream().use { it.read() == 'P'.code && it.read() == 'K'.code }
        if (zip) {
            ZipFile(file).use { archive ->
                val entries = archive.entries()
                var count = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    require(++count <= 10000) { "Too many files in Takeout archive." }
                    val path = entry.name.lowercase(Locale.ROOT)
                    val playlist = path.endsWith(".csv") && (path.contains("/playlists/") || path.endsWith("-videos.csv"))
                    val watch = path.endsWith("watch-history.json") || path.endsWith("watch-history.html")
                    if (!entry.isDirectory && (playlist || watch)) archive.getInputStream(entry).use { read(entry.name, it) }
                }
            }
        } else {
            file.inputStream().use { read(name, it) }
        }
        require(playlists.isNotEmpty() || history.isNotEmpty()) { "No supported playlists or watch history found. Select playlist CSV, watch-history JSON/HTML, or a Takeout ZIP. Export history as JSON for reliable dates." }
        return TakeoutData(playlists, history, skipped)
    }

    private fun video(value: String, title: String): TakeoutVideo? = runCatching {
        val factory = YoutubeStreamLinkHandlerFactory.getInstance()
        val id = if (value.matches(Regex("[A-Za-z0-9_-]{11}"))) value else factory.getId(value)
        TakeoutVideo(factory.getUrl(id), title.ifBlank { id }.take(1000))
    }.getOrNull()

    private fun htmlTime(value: String): Long? {
        val text = value.trim().replace('\u202f', ' ').replace('\u00a0', ' ')
        return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull() ?: listOf(
            "MMM d, uuuu, h:mm:ss a z", "MMM d, uuuu, HH:mm:ss z", "d MMM uuuu, HH:mm:ss z"
        ).firstNotNullOfOrNull { pattern ->
            runCatching { ZonedDateTime.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.US)).toInstant().toEpochMilli() }.getOrNull()
        }
    }

    /** RFC 4180 quoted fields, embedded newlines, doubled quotes, CRLF and empty columns. */
    internal fun csv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val char = text[i++]
            when {
                char == '"' && quoted && i < text.length && text[i] == '"' -> {
                    value.append('"')
                    i++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    row.add(value.toString())
                    require(row.size <= 1000) { "Too many CSV columns." }
                    value.setLength(0)
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    row.add(value.toString())
                    rows.add(row)
                    require(rows.size <= MAX_RECORDS + 100) { "Too many CSV rows." }
                    row = mutableListOf()
                    value.setLength(0)
                    if (char == '\r' && i < text.length && text[i] == '\n') i++
                }
                else -> value.append(char)
            }
        }
        require(!quoted) { "Unclosed quoted CSV field." }
        if (row.isNotEmpty() || value.isNotEmpty()) {
            row.add(value.toString())
            rows.add(row)
        }
        return rows
    }
}
