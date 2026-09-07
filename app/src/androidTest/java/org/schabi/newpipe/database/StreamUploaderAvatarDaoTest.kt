package org.schabi.newpipe.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.database.stream.dao.StreamDAO
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamType

class StreamUploaderAvatarDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var streamDao: StreamDAO
    private val youtubeServiceId = ServiceList.YouTube.serviceId
    private val sharedUploader = "https://youtube.com/channel/shared"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        streamDao = db.streamDAO()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun knownAvatarRepairsOnlyMatchingMissingRows() {
        streamDao.insertAll(
            listOf(
                stream("known", sharedUploader, "https://example.com/known.jpg"),
                stream("missing", sharedUploader, null),
                stream("existing", sharedUploader, "https://example.com/existing.jpg"),
                stream("other", "https://youtube.com/channel/other", null)
            )
        )

        assertEquals(
            "https://example.com/known.jpg",
            streamDao.findKnownUploaderAvatar(youtubeServiceId, sharedUploader)
        )
        assertEquals(
            1,
            streamDao.backfillMissingUploaderAvatar(
                youtubeServiceId,
                sharedUploader,
                "https://example.com/resolved.jpg"
            )
        )

        assertEquals(
            "https://example.com/resolved.jpg",
            streamDao.getStreamDirect(
                youtubeServiceId,
                "https://youtube.com/watch?v=missing"
            )?.uploaderAvatarUrl
        )
        assertEquals(
            "https://example.com/known.jpg",
            streamDao.getStreamDirect(
                youtubeServiceId,
                "https://youtube.com/watch?v=known"
            )?.uploaderAvatarUrl
        )
        assertEquals(
            "https://example.com/existing.jpg",
            streamDao.getStreamDirect(
                youtubeServiceId,
                "https://youtube.com/watch?v=existing"
            )?.uploaderAvatarUrl
        )
        assertNull(
            streamDao.getStreamDirect(
                youtubeServiceId,
                "https://youtube.com/watch?v=other"
            )?.uploaderAvatarUrl
        )
    }

    private fun stream(id: String, uploaderUrl: String, avatarUrl: String?): StreamEntity {
        return StreamEntity(
            serviceId = youtubeServiceId,
            url = "https://youtube.com/watch?v=$id",
            title = id,
            streamType = StreamType.VIDEO_STREAM,
            duration = 60,
            uploader = "Uploader",
            uploaderUrl = uploaderUrl,
            uploaderAvatarUrl = avatarUrl
        )
    }
}
