package org.thoughtcrime.securesms.ryan.database;

import android.database.Cursor;

import androidx.annotation.NonNull;

import net.zetetic.database.DatabaseErrorHandler;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.signal.core.util.CursorUtil;

/**
 * Prints some diagnostics and then deletes the original so the database can be recreated.
 * This should only be used for database that contain "unimportant" data, like logs.
 * Otherwise, you should use {@link SqlCipherErrorHandler}.
 */
public final class SqlCipherDeletingErrorHandler implements DatabaseErrorHandler {

  private static final String TAG = Log.tag(SqlCipherDeletingErrorHandler.class);

  private final String databaseName;

  public SqlCipherDeletingErrorHandler(@NonNull String databaseName) {
    this.databaseName = databaseName;
  }

  @Override
  public void onCorruption(SQLiteDatabase db, String message) {
    try {
      Log.e(TAG, "Database '" + databaseName + "' corrupted! Message: " + message + ". Going to try to run some diagnostics.");

      Log.w(TAG, " ===== PRAGMA integrity_check =====");
      try (Cursor cursor = db.rawQuery("PRAGMA integrity_check", null)) {
        while (cursor.moveToNext()) {
          Log.w(TAG, CursorUtil.readRowAsString(cursor));
        }
      } catch (Throwable t) {
        Log.e(TAG, "Failed to do integrity_check!", t);
      }

      Log.w(TAG, "===== PRAGMA cipher_integrity_check =====");
      try (Cursor cursor = db.rawQuery("PRAGMA cipher_integrity_check", null)) {
        while (cursor.moveToNext()) {
          Log.w(TAG, CursorUtil.readRowAsString(cursor));
        }
      } catch (Throwable t) {
        Log.e(TAG, "Failed to do cipher_integrity_check!", t);
      }
    } catch (Throwable t) {
      Log.e(TAG, "Failed to run diagnostics!", t);
    } finally {
      Log.w(TAG, "Deleting database " + databaseName);
      AppDependencies.getApplication().deleteDatabase(databaseName);
    }
  }
}
