/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.backup.v2.processor

import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.backup.v2.ExportState
import org.thoughtcrime.securesms.ryan.backup.v2.ImportState
import org.thoughtcrime.securesms.ryan.backup.v2.MessageBackupTier
import org.thoughtcrime.securesms.ryan.backup.v2.database.getThreadsForBackup
import org.thoughtcrime.securesms.ryan.backup.v2.importer.ChatArchiveImporter
import org.thoughtcrime.securesms.ryan.backup.v2.proto.Chat
import org.thoughtcrime.securesms.ryan.backup.v2.proto.Frame
import org.thoughtcrime.securesms.ryan.backup.v2.stream.BackupFrameEmitter
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.recipients.RecipientId

/**
 * Handles importing/exporting [Chat] frames for an archive.
 */
object ChatArchiveProcessor {
  val TAG = Log.tag(ChatArchiveProcessor::class.java)

  fun export(db: SignalDatabase, exportState: ExportState, emitter: BackupFrameEmitter) {
    val includeImageWallpapers = SignalStore.backup.backupTier == MessageBackupTier.PAID
    Log.i(TAG, "Including wallpapers: $includeImageWallpapers")

    db.threadTable.getThreadsForBackup(db, includeImageWallpapers).use { reader ->
      for (chat in reader) {
        if (exportState.recipientIds.contains(chat.recipientId)) {
          exportState.threadIds.add(chat.id)
          exportState.threadIdToRecipientId[chat.id] = chat.recipientId
          emitter.emit(Frame(chat = chat))
        } else {
          Log.w(TAG, "dropping thread for deleted recipient ${chat.recipientId}")
        }
      }
    }
  }

  fun import(chat: Chat, importState: ImportState) {
    val recipientId: RecipientId? = importState.remoteToLocalRecipientId[chat.recipientId]
    if (recipientId == null) {
      Log.w(TAG, "Missing recipient for chat ${chat.id}")
      return
    }

    val threadId = ChatArchiveImporter.import(chat, recipientId, importState)
    importState.chatIdToLocalRecipientId[chat.id] = recipientId
    importState.chatIdToLocalThreadId[chat.id] = threadId
    importState.chatIdToBackupRecipientId[chat.id] = chat.recipientId
    importState.recipientIdToLocalThreadId[recipientId] = threadId
  }
}
