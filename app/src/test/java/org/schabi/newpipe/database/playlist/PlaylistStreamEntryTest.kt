package org.schabi.newpipe.database.playlist

import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamType

class PlaylistStreamEntryTest {
    @Test
    fun toStreamInfoItemCarriesStoredUploaderAvatar() {
        val stream = StreamEntity(
            serviceId = ServiceList.YouTube.serviceId,
            url = "https://youtube.com/watch?v=abcdefghijk",
            title = "Video",
            streamType = StreamType.VIDEO_STREAM,
            duration = 60,
            uploader = "Uploader",
            uploaderUrl = "https://youtube.com/channel/test",
            uploaderAvatarUrl = "https://example.com/avatar.jpg"
        )
        val entry = PlaylistStreamEntry(
            streamEntity = stream,
            progressMillis = 0,
            streamId = 1,
            joinIndex = 0
        )

        assertEquals(
            "https://example.com/avatar.jpg",
            entry.toStreamInfoItem().uploaderAvatarUrl
        )
    }
}
