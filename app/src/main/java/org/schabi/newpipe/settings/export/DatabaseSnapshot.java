package org.schabi.newpipe.settings.export;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Copies a transactional view, including WAL content, without closing the live database. */
public final class DatabaseSnapshot {
    private DatabaseSnapshot() {
    }

    public static void create(final SupportSQLiteDatabase source, final File destination) {
        source.beginTransactionNonExclusive();
        try (SQLiteDatabase target = SQLiteDatabase.openOrCreateDatabase(destination, null)) {
            target.beginTransaction();
            try {
                final List<String> tables = new ArrayList<>();
                try (Cursor schema = source.query("SELECT name, sql FROM sqlite_master"
                        + " WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
                        + " AND name != 'android_metadata'")) {
                    while (schema.moveToNext()) {
                        target.execSQL(schema.getString(1));
                        tables.add(schema.getString(0));
                    }
                }
                for (final String table : tables) {
                    copyTable(source, target, table);
                }
                // AUTOINCREMENT sequences can exceed the largest surviving row ID.
                try (Cursor sequence = source.query("SELECT name FROM sqlite_master"
                        + " WHERE name = 'sqlite_sequence'")) {
                    if (sequence.moveToFirst()) {
                        target.execSQL("DELETE FROM sqlite_sequence");
                        copyTable(source, target, "sqlite_sequence");
                    }
                }
                try (Cursor schema = source.query("SELECT sql FROM sqlite_master"
                        + " WHERE type IN ('index', 'trigger', 'view') AND sql IS NOT NULL")) {
                    while (schema.moveToNext()) {
                        target.execSQL(schema.getString(0));
                    }
                }
                target.setVersion(source.getVersion());
                target.setTransactionSuccessful();
            } finally {
                target.endTransaction();
            }
        } finally {
            source.endTransaction();
        }
    }

    private static void copyTable(final SupportSQLiteDatabase source,
                                  final SQLiteDatabase target, final String table) {
        final String quoted = "\"" + table.replace("\"", "\"\"") + "\"";
        try (Cursor rows = source.query("SELECT * FROM " + quoted)) {
            final String placeholders = String.join(",",
                    java.util.Collections.nCopies(rows.getColumnCount(), "?"));
            final String insert = "INSERT INTO " + quoted + " VALUES (" + placeholders + ")";
            while (rows.moveToNext()) {
                final Object[] values = new Object[rows.getColumnCount()];
                for (int column = 0; column < values.length; column++) {
                    switch (rows.getType(column)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            values[column] = rows.getLong(column);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            values[column] = rows.getDouble(column);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            values[column] = rows.getBlob(column);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            values[column] = rows.getString(column);
                            break;
                        default:
                            values[column] = null;
                    }
                }
                target.execSQL(insert, values);
            }
        }
    }
}
