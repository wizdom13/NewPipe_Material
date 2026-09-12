package org.schabi.newpipe.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.schabi.newpipe.settings.export.DatabaseSnapshot;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class DatabaseSnapshotTest {
    @Test
    public void snapshotIncludesWalRowsSchemaVersionAndSequences() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        final File snapshot = File.createTempFile("snapshot-test", ".db", context.getCacheDir());
        final SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name("snapshot-source-test.db")
                        .callback(new SupportSQLiteOpenHelper.Callback(23) {
                            @Override
                            public void onCreate(@NonNull final SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY"
                                        + " AUTOINCREMENT, title TEXT, data BLOB, score REAL)");
                                db.execSQL("CREATE INDEX item_title ON items(title)");
                            }

                            @Override
                            public void onUpgrade(@NonNull final SupportSQLiteDatabase db,
                                                  final int oldVersion, final int newVersion) {
                            }
                        }).build();
        try (SupportSQLiteOpenHelper helper =
                     new FrameworkSQLiteOpenHelperFactory().create(configuration)) {
            helper.setWriteAheadLoggingEnabled(true);
            final SupportSQLiteDatabase source = helper.getWritableDatabase();
            source.execSQL("INSERT INTO items VALUES (41, 'kept', X'0102', 1.5)");
            source.execSQL("INSERT INTO items VALUES (99, NULL, NULL, NULL)");
            source.execSQL("DELETE FROM items WHERE id = 99");
            DatabaseSnapshot.create(source, snapshot);
            source.execSQL("DELETE FROM items");
            try (SQLiteDatabase restored = SQLiteDatabase.openOrCreateDatabase(snapshot, null)) {
                assertEquals(23, restored.getVersion());
                try (Cursor rows = restored.rawQuery("SELECT * FROM items", null)) {
                    assertTrue(rows.moveToFirst());
                    assertEquals(41, rows.getLong(0));
                    assertEquals("kept", rows.getString(1));
                    assertEquals(2, rows.getBlob(2).length);
                    assertEquals(1.5, rows.getDouble(3), 0.0);
                }
                restored.execSQL("INSERT INTO items(title) VALUES ('next')");
                try (Cursor rows = restored.rawQuery("SELECT MAX(id) FROM items", null)) {
                    assertTrue(rows.moveToFirst());
                    assertEquals(100, rows.getLong(0));
                }
                try (Cursor check = restored.rawQuery("PRAGMA integrity_check", null)) {
                    assertTrue(check.moveToFirst());
                    assertEquals("ok", check.getString(0));
                }
            }
        } finally {
            context.deleteDatabase("snapshot-source-test.db");
            SQLiteDatabase.deleteDatabase(snapshot);
        }
    }
}
