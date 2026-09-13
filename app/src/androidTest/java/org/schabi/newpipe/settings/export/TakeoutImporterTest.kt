package org.schabi.newpipe.settings.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.schabi.newpipe.database.AppDatabase

class TakeoutImporterTest {
    @Test
    fun repeatedImportsPreservePlaylistOrderAndWatchDatesWithoutDuplicates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val first = TakeoutVideo("https://www.youtube.com/watch?v=abcdefghijk", "First")
            val second = TakeoutVideo("https://www.youtube.com/watch?v=lmnopqrstuv", "Second")
            val data = TakeoutData(listOf(TakeoutPlaylist("Learning", listOf(second, first))), listOf(TakeoutWatch(first, 1700000000123)), 0)
            var journaled = 0
            val importer = TakeoutImporter(db) { _, _, _ -> journaled++ }
            assertEquals(TakeoutImportResult(1, 2, 1, 0), importer.import(data))
            assertEquals(TakeoutImportResult(0, 0, 0, 3), importer.import(data))
            val playlist = db.playlistDAO().getAllDirect().single()
            assertEquals(listOf(second.url, first.url), db.playlistStreamDAO().getOrderedStreamsDirect(playlist.uid).map { it.url })
            assertEquals(1700000000123, db.streamHistoryDAO().getAllDirect().single().accessDate.toInstant().toEpochMilli())
            assertEquals(1, journaled)
        } finally {
            db.close()
        }
    }

    @Test
    fun failedHistoryWriteRollsBackPlaylistsAndStreamsAsWell() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val video = TakeoutVideo("https://www.youtube.com/watch?v=abcdefghijk", "First")
            val data = TakeoutData(listOf(TakeoutPlaylist("Learning", listOf(video))), listOf(TakeoutWatch(video, 1700000000123)), 0)
            val importer = TakeoutImporter(db) { _, _, _ -> throw IllegalStateException("Simulated storage failure") }
            assertThrows(IllegalStateException::class.java) { importer.import(data) }
            assertEquals(0, db.playlistDAO().getAllDirect().size)
            assertEquals(0, db.streamHistoryDAO().getAllDirect().size)
            assertEquals(0, db.streamDAO().getAll().blockingFirst().size)
        } finally {
            db.close()
        }
    }
}
