package org.thoughtcrime.securesms.ryan.components.settings.app.chats

data class ChatsSettingsState(
  val generateLinkPreviews: Boolean,
  val useAddressBook: Boolean,
  val keepMutedChatsArchived: Boolean,
  val useSystemEmoji: Boolean,
  val enterKeySends: Boolean,
  val localBackupsEnabled: Boolean,
  val folderCount: Int
)
