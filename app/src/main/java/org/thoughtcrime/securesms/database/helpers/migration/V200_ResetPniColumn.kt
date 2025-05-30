/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.database.helpers.migration

import android.app.Application
import org.thoughtcrime.securesms.ryan.database.SQLiteDatabase

/**
 * This updates the PNI column to have the proper serialized format.
 */
@Suppress("ClassName")
object V200_ResetPniColumn : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE recipient SET pni = 'PNI:' || pni WHERE pni NOT NULL")
    db.execSQL("ALTER TABLE recipient RENAME COLUMN uuid to aci")
  }
}
