package org.schabi.newpipe.local.feed.service

import java.time.OffsetDateTime
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfoItem

internal object FeedItemDateResolver {
    /**
     * Gives undated Shorts a deterministic order and lets the feed database retain them.
     *
     * YouTube's Shorts tab does not expose upload dates. The feed normally rejects undated
     * non-live items so that it can order and expire them. A first-seen date is the closest useful
     * approximation: [StreamDAO][org.schabi.newpipe.database.stream.dao.StreamDAO] preserves it on
     * later refreshes, even when another approximate value is supplied.
     *
     * @param streams items returned by one feed extraction, in newest-first source order
     * @param fetchedAt time at which the extraction completed
     */
    fun applyFirstSeenDates(streams: List<StreamInfoItem>, fetchedAt: OffsetDateTime) {
        var undatedShortIndex = 0L

        streams.forEach { stream ->
            if (stream.uploadDate == null && stream.isShortFormContent) {
                stream.uploadDate = DateWrapper(
                    fetchedAt.minusSeconds(undatedShortIndex),
                    true
                )
                undatedShortIndex++
            }
        }
    }
}
