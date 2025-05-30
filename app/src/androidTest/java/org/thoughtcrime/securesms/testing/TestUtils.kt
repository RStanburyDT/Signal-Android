package org.thoughtcrime.securesms.ryan.testing

import android.database.Cursor
import org.signal.core.util.Hex
import org.signal.core.util.logging.Log
import org.signal.core.util.readToList
import org.signal.core.util.select
import org.thoughtcrime.securesms.ryan.database.MessageTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.util.MessageTableTestUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/**
 * Run the given [runnable] on a new thread and wait for it to finish.
 */
fun runSync(runnable: () -> Unit) {
  val lock = CountDownLatch(1)
  Thread {
    try {
      runnable.invoke()
    } finally {
      lock.countDown()
    }
  }.start()
  lock.await()
}

fun CountDownLatch.awaitFor(duration: Duration) {
  if (!await(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
    throw TimeoutException("Latch await took longer than ${duration.inWholeMilliseconds}ms")
  }
}

fun dumpTableToLogs(tag: String = "TestUtils", table: String, columns: Set<String>? = null) {
  dumpTable(table, columns).forEach { Log.d(tag, it.toString()) }
}

fun dumpTable(table: String, columns: Set<String>?): List<List<Pair<String, String?>>> {
  return SignalDatabase.rawDatabase
    .select()
    .from(table)
    .run()
    .readToList { cursor ->
      val map: List<Pair<String, String?>> = cursor.columnNames.mapNotNull { column ->
        if (columns == null || columns.contains(column)) {
          val index = cursor.getColumnIndex(column)
          var data: String? = when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_BLOB -> Hex.toStringCondensed(cursor.getBlob(index))
            else -> cursor.getString(index)
          }
          if (table == MessageTable.TABLE_NAME && column == MessageTable.TYPE) {
            data = MessageTableTestUtils.typeColumnToString(cursor.getLong(index))
          }

          column to data
        } else {
          null
        }
      }
      map
    }
}
