package org.schabi.newpipe.settings.export

import java.time.Instant
import java.time.ZoneOffset
import org.schabi.newpipe.database.AppDatabase
import org.schabi.newpipe.database.history.model.StreamHistoryEntity
import org.schabi.newpipe.database.playlist.model.PlaylistEntity
import org.schabi.newpipe.database.playlist.model.PlaylistStreamEntity
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.stream.StreamType

internal data class TakeoutImportResult(val playlists: Int, val videos: Int, val watches: Int, val skipped: Int)

internal class TakeoutImporter(private val database: AppDatabase, private val recordWatch: (Long, Long, Long) -> Unit) {
    fun import(data: TakeoutData): TakeoutImportResult {
        var playlists = 0
        var videos = 0
        var watches = 0
        var skipped = data.skipped
        database.runInTransaction {
            fun stream(video: TakeoutVideo): Long = database.streamDAO().getStreamDirect(0, video.url)?.uid ?: database.streamDAO().insert(
                StreamEntity(serviceId = 0, url = video.url, title = video.title, streamType = StreamType.VIDEO_STREAM, duration = -1, uploader = "")
            )
            val existing = database.playlistDAO().getAllDirect().associateBy { it.name }.toMutableMap()
            for (playlist in data.playlists) {
                check(!Thread.currentThread().isInterrupted) { "Import cancelled" }
                val name = "${playlist.name.take(200)} (Takeout)"
                var target = existing[name]
                val old = target?.let { database.playlistStreamDAO().getOrderedStreamsDirect(it.uid) }.orEmpty()
                val seen = old.map { it.url }.toMutableSet()
                var index = old.size
                for (video in playlist.videos) {
                    if (!seen.add(video.url)) {
                        skipped++
                        continue
                    }
                    val streamId = stream(video)
                    if (target == null) {
                        target = PlaylistEntity(name = name, isThumbnailPermanent = false, thumbnailStreamId = streamId, displayIndex = -1)
                        target.uid = database.playlistDAO().insert(target)
                        existing[name] = target
                        playlists++
                    }
                    database.playlistStreamDAO().insert(PlaylistStreamEntity(target.uid, streamId, index++))
                    videos++
                }
            }
            for (watch in data.history) {
                check(!Thread.currentThread().isInterrupted) { "Import cancelled" }
                val streamId = stream(watch.video)
                val date = Instant.ofEpochMilli(watch.timestamp).atOffset(ZoneOffset.UTC)
                if (database.streamHistoryDAO().hasTakeoutEvent(streamId, date)) {
                    skipped++
                } else {
                    // Initialize and journal before materializing, as normal playback does.
                    recordWatch(streamId, watch.timestamp, 1)
                    database.streamHistoryDAO().insert(StreamHistoryEntity(streamId, date, 1))
                    watches++
                }
            }
        }
        return TakeoutImportResult(playlists, videos, watches, skipped)
    }
}
