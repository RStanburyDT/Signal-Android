package org.thoughtcrime.securesms.ryan.database.helpers.migration

import android.app.Application
import org.thoughtcrime.securesms.ryan.database.SQLiteDatabase

/**
 * In order to support scheduled sending, we need to add another column to keep track of when to send the message. We also use this
 * column to hide future scheduled messages from views.
 */
@Suppress("ClassName")
object V173_ScheduledMessagesMigration : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE mms ADD COLUMN scheduled_date INTEGER DEFAULT -1")
    db.execSQL("DROP INDEX mms_thread_story_parent_story_index")
    db.execSQL(
      "CREATE INDEX IF NOT EXISTS mms_thread_story_parent_story_scheduled_date_index ON mms (thread_id, date_received,story_type,parent_story_id,scheduled_date);"
    )
  }
}
