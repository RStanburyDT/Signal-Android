package org.thoughtcrime.securesms.ryan.database.helpers.migration

import android.app.Application
import org.thoughtcrime.securesms.ryan.database.SQLiteDatabase

/**
 * Track last time we did a forced sanity check for this group with the server.
 */
@Suppress("ClassName")
object V158_GroupsLastForceUpdateTimestampMigration : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE groups ADD COLUMN last_force_update_timestamp INTEGER DEFAULT 0")
  }
}
