package org.thoughtcrime.securesms.ryan.components.settings.app.chats.folders

import org.thoughtcrime.securesms.ryan.contacts.paged.ChatType
import org.thoughtcrime.securesms.ryan.recipients.RecipientId

/**
 * Information about chat folders. Used in [ChatFoldersViewModel].
 */
data class ChatFoldersSettingsState(
  val folders: List<ChatFolderRecord> = emptyList(),
  val suggestedFolders: List<ChatFolderRecord> = emptyList(),
  val originalFolder: ChatFolder = ChatFolder(),
  val currentFolder: ChatFolder = ChatFolder(),
  val showDeleteDialog: Boolean = false,
  val showConfirmationDialog: Boolean = false,
  val pendingIncludedRecipients: Set<RecipientId> = emptySet(),
  val pendingExcludedRecipients: Set<RecipientId> = emptySet(),
  val pendingChatTypes: Set<ChatType> = emptySet()
)
