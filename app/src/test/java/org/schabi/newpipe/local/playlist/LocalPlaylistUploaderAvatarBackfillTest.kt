package org.schabi.newpipe.local.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.database.playlist.PlaylistStreamEntry
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamType

class LocalPlaylistUploaderAvatarBackfillTest {
    private val youtubeServiceId = ServiceList.YouTube.serviceId

    @Test
    fun missingUploaderKeysGroupsRowsAndIgnoresKnownAndLocalMedia() {
        val entries = listOf(
            entry("one", youtubeServiceId, "https://youtube.com/channel/shared", null),
            entry("two", youtubeServiceId, "https://youtube.com/channel/shared", ""),
            entry(
                "three",
                youtubeServiceId,
                "https://youtube.com/channel/known",
                "https://example.com/avatar.jpg"
            ),
            entry(
                "local",
                youtubeServiceId,
                "https://youtube.com/channel/local",
                null,
                StreamEntity.SOURCE_TYPE_LOCAL
            )
        )

        assertEquals(
            listOf(
                LocalPlaylistUploaderKey(
                    youtubeServiceId,
                    "https://youtube.com/channel/shared"
                )
            ),
            missingUploaderKeys(entries)
        )
    }

    @Test
    fun networkLookupRequiresExactlyOneYoutubeUploader() {
        val youtube = LocalPlaylistUploaderKey(
            youtubeServiceId,
            "https://youtube.com/channel/shared"
        )
        val otherYoutube = LocalPlaylistUploaderKey(
            youtubeServiceId,
            "https://youtube.com/channel/other"
        )
        val otherService = LocalPlaylistUploaderKey(
            youtubeServiceId + 1,
            "https://example.com/channel"
        )

        assertEquals(youtube, selectYoutubeUploaderLookupCandidate(listOf(youtube)))
        assertNull(selectYoutubeUploaderLookupCandidate(listOf(youtube, otherYoutube)))
        assertNull(selectYoutubeUploaderLookupCandidate(listOf(otherService)))
    }

    @Test
    fun applyUploaderAvatarFillsOnlyMatchingMissingRows() {
        val key = LocalPlaylistUploaderKey(
            youtubeServiceId,
            "https://youtube.com/channel/shared"
        )
        val missing = entry("one", youtubeServiceId, key.uploaderUrl, null)
        val existing = entry(
            "two",
            youtubeServiceId,
            key.uploaderUrl,
            "https://example.com/existing.jpg"
        )
        val other = entry(
            "three",
            youtubeServiceId,
            "https://youtube.com/channel/other",
            null
        )
        val entries = listOf(missing, existing, other)

        applyUploaderAvatar(entries, key, "https://example.com/resolved.jpg")

        assertEquals(
            "https://example.com/resolved.jpg",
            missing.streamEntity.uploaderAvatarUrl
        )
        assertEquals(
            "https://example.com/existing.jpg",
            existing.streamEntity.uploaderAvatarUrl
        )
        assertNull(other.streamEntity.uploaderAvatarUrl)
    }

    private fun entry(
        id: String,
        serviceId: Int,
        uploaderUrl: String?,
        avatarUrl: String?,
        sourceType: String = StreamEntity.SOURCE_TYPE_REMOTE
    ): PlaylistStreamEntry {
        val stream = StreamEntity(
            serviceId = serviceId,
            url = "https://example.com/$id",
            title = id,
            streamType = StreamType.VIDEO_STREAM,
            duration = 60,
            uploader = "Uploader",
            uploaderUrl = uploaderUrl,
            uploaderAvatarUrl = avatarUrl,
            sourceType = sourceType
        )
        return PlaylistStreamEntry(
            streamEntity = stream,
            progressMillis = 0,
            streamId = 0,
            joinIndex = 0
        )
    }
}
