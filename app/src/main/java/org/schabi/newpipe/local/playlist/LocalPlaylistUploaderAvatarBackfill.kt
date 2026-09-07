/*
 * SPDX-FileCopyrightText: 2026 WizeStream contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.local.playlist

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.database.AppDatabase
import org.schabi.newpipe.database.playlist.PlaylistStreamEntry
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.util.ExtractorHelper
import org.schabi.newpipe.util.image.ImageStrategy

internal data class LocalPlaylistUploaderKey(
    val serviceId: Int,
    val uploaderUrl: String
)

/** Repairs missing uploader avatars already stored in local playlists. */
object LocalPlaylistUploaderAvatarBackfill {
    @JvmStatic
    fun backfill(
        database: AppDatabase,
        entries: List<PlaylistStreamEntry>
    ): Completable = Completable.fromAction {
        val streamDao = database.streamDAO()
        val unresolved = missingUploaderKeys(entries).filterTo(mutableListOf()) { key ->
            val knownAvatar = streamDao.findKnownUploaderAvatar(
                key.serviceId,
                key.uploaderUrl
            )
            if (knownAvatar.isNullOrBlank()) {
                true
            } else {
                applyUploaderAvatar(entries, key, knownAvatar)
                streamDao.backfillMissingUploaderAvatar(
                    key.serviceId,
                    key.uploaderUrl,
                    knownAvatar
                )
                false
            }
        }

        selectYoutubeUploaderLookupCandidate(unresolved)?.let { key ->
            val resolvedAvatar = runCatching {
                val channel = ExtractorHelper.getChannelInfo(
                    key.serviceId,
                    key.uploaderUrl,
                    false
                ).blockingGet()
                ImageStrategy.imageListToDbUrl(channel.avatars)
            }.getOrNull()

            if (!resolvedAvatar.isNullOrBlank()) {
                applyUploaderAvatar(entries, key, resolvedAvatar)
                streamDao.backfillMissingUploaderAvatar(
                    key.serviceId,
                    key.uploaderUrl,
                    resolvedAvatar
                )
            }
        }
    }.subscribeOn(Schedulers.io())
}

internal fun missingUploaderKeys(
    entries: List<PlaylistStreamEntry>
): List<LocalPlaylistUploaderKey> = entries.asSequence()
    .map(PlaylistStreamEntry::streamEntity)
    .filterNot { it.isLocalMedia }
    .filter { it.uploaderAvatarUrl.isNullOrBlank() }
    .mapNotNull { stream ->
        stream.uploaderUrl
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { LocalPlaylistUploaderKey(stream.serviceId, it) }
    }
    .distinct()
    .toList()

internal fun selectYoutubeUploaderLookupCandidate(
    unresolved: List<LocalPlaylistUploaderKey>
): LocalPlaylistUploaderKey? = unresolved.singleOrNull()
    ?.takeIf { it.serviceId == ServiceList.YouTube.serviceId }

internal fun applyUploaderAvatar(
    entries: List<PlaylistStreamEntry>,
    key: LocalPlaylistUploaderKey,
    avatarUrl: String
) {
    entries.asSequence()
        .map(PlaylistStreamEntry::streamEntity)
        .filter { stream ->
            stream.serviceId == key.serviceId &&
                stream.uploaderUrl == key.uploaderUrl &&
                stream.uploaderAvatarUrl.isNullOrBlank()
        }
        .forEach { it.uploaderAvatarUrl = avatarUrl }
}
