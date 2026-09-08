package org.schabi.newpipe.local.feed.service

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class FeedItemDateResolverTest {
    private val fetchedAt = OffsetDateTime.of(2026, 9, 8, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `undated shorts receive ordered approximate first seen dates`() {
        val first = stream("https://youtube.com/shorts/first", isShort = true)
        val second = stream("https://youtube.com/shorts/second", isShort = true)

        val shouldReposition =
            FeedItemDateResolver.applyApproximateDates(listOf(first, second), fetchedAt)

        assertEquals(fetchedAt, first.uploadDate!!.offsetDateTime())
        assertEquals(fetchedAt.minusSeconds(1), second.uploadDate!!.offsetDateTime())
        assertTrue(first.uploadDate!!.isApproximation)
        assertTrue(second.uploadDate!!.isApproximation)
        assertFalse(shouldReposition)
    }

    @Test
    fun `existing dates and undated regular videos are unchanged`() {
        val originalDate = DateWrapper(fetchedAt.minusDays(1))
        val datedShort = stream("https://youtube.com/shorts/dated", isShort = true).apply {
            uploadDate = originalDate
        }
        val regularVideo = stream("https://youtube.com/watch?v=regular", isShort = false)

        FeedItemDateResolver.applyApproximateDates(listOf(datedShort, regularVideo), fetchedAt)

        assertEquals(originalDate, datedShort.uploadDate)
        assertFalse(datedShort.uploadDate!!.isApproximation)
        assertNull(regularVideo.uploadDate)
    }

    @Test
    fun `undated shorts are distributed between dated channel items`() {
        val newestVideo = stream("https://youtube.com/watch?v=newest", isShort = false).apply {
            uploadDate = DateWrapper(fetchedAt.minusDays(1))
        }
        val oldestVideo = stream("https://youtube.com/watch?v=oldest", isShort = false).apply {
            uploadDate = DateWrapper(fetchedAt.minusDays(11))
        }
        val firstShort = stream("https://youtube.com/shorts/first", isShort = true)
        val secondShort = stream("https://youtube.com/shorts/second", isShort = true)
        val thirdShort = stream("https://youtube.com/shorts/third", isShort = true)

        val shouldReposition = FeedItemDateResolver.applyApproximateDates(
            listOf(newestVideo, oldestVideo, firstShort, secondShort, thirdShort),
            fetchedAt
        )

        assertEquals(
            fetchedAt.minusDays(3).minusHours(12),
            firstShort.uploadDate!!.offsetDateTime()
        )
        assertEquals(fetchedAt.minusDays(6), secondShort.uploadDate!!.offsetDateTime())
        assertEquals(
            fetchedAt.minusDays(8).minusHours(12),
            thirdShort.uploadDate!!.offsetDateTime()
        )
        assertTrue(firstShort.uploadDate!!.isApproximation)
        assertTrue(
            firstShort.uploadDate!!.offsetDateTime() > secondShort.uploadDate!!.offsetDateTime()
        )
        assertTrue(
            secondShort.uploadDate!!.offsetDateTime() > thirdShort.uploadDate!!.offsetDateTime()
        )
        assertTrue(shouldReposition)
    }

    private fun stream(url: String, isShort: Boolean) = StreamInfoItem(
        0,
        url,
        "Title",
        StreamType.VIDEO_STREAM
    ).apply {
        setShortFormContent(isShort)
    }
}
