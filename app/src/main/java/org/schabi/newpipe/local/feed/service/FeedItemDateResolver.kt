package org.schabi.newpipe.local.feed.service

import java.time.Duration
import java.time.OffsetDateTime
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfoItem

internal object FeedItemDateResolver {
    /**
     * Gives undated Shorts approximate dates so the feed database can retain and order them.
     *
     * YouTube's Shorts tab does not expose upload dates. The feed normally rejects undated
     * non-live items so that it can order and expire them. When dated items are available from the
     * same channel refresh, distribute Shorts across that date range instead of promoting every
     * Short above all regular videos. If no dated item is available, first-seen order is the closest
     * useful approximation. Approximate dates can be repositioned on later refreshes without
     * replacing any exact upload date.
     *
     * @param streams items returned by one feed extraction, in newest-first source order
     * @param fetchedAt time at which the extraction completed
     * @return whether stored approximate Short dates should be repositioned to the inferred range
     */
    fun applyApproximateDates(streams: List<StreamInfoItem>, fetchedAt: OffsetDateTime): Boolean {
        val undatedShorts = streams.filter { it.uploadDate == null && it.isShortFormContent }
        if (undatedShorts.isEmpty()) {
            return false
        }

        val datedItems = streams
            .mapNotNull { it.uploadDate?.offsetDateTime() }
            .filterNot { it.isAfter(fetchedAt) }
        val newestKnownDate = datedItems.maxOrNull()
        val oldestKnownDate = datedItems.minOrNull()
        val knownRangeSeconds = if (newestKnownDate != null && oldestKnownDate != null) {
            Duration.between(oldestKnownDate, newestKnownDate).seconds
        } else {
            0L
        }

        undatedShorts.forEachIndexed { index, stream ->
            val approximateDate = if (newestKnownDate != null && knownRangeSeconds > 0) {
                val position = index + 1L
                val slots = undatedShorts.size + 1L
                newestKnownDate.minusSeconds(maxOf(1L, knownRangeSeconds * position / slots))
            } else {
                val offset = index.toLong() + if (newestKnownDate == null) 0L else 1L
                (newestKnownDate ?: fetchedAt).minusSeconds(offset)
            }
            stream.uploadDate = DateWrapper(approximateDate, true)
        }
        return newestKnownDate != null
    }
}
