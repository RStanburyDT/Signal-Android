package org.thoughtcrime.securesms.ryan.components.settings.app.chats.folders

import org.thoughtcrime.securesms.ryan.recipients.Recipient

/**
 * Information needed when creating/updating a chat folder. Used in [ChatFoldersSettingsState]
 */
data class ChatFolder(
  val folderRecord: ChatFolderRecord = ChatFolderRecord(),
  val includedRecipients: Set<Recipient> = emptySet(),
  val excludedRecipients: Set<Recipient> = emptySet()
)
