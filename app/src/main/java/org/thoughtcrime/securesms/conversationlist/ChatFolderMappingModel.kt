package org.thoughtcrime.securesms.ryan.conversationlist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.thoughtcrime.securesms.ryan.components.settings.app.chats.folders.ChatFolderRecord
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

/**
 * Mapping model of folders used in [ChatFolderAdapter]
 */
@Parcelize
data class ChatFolderMappingModel(
  val chatFolder: ChatFolderRecord,
  val unreadCount: Int,
  val isMuted: Boolean,
  val isSelected: Boolean
) : MappingModel<ChatFolderMappingModel>, Parcelable {
  override fun areItemsTheSame(newItem: ChatFolderMappingModel): Boolean {
    return chatFolder.id == newItem.chatFolder.id
  }

  override fun areContentsTheSame(newItem: ChatFolderMappingModel): Boolean {
    return chatFolder == newItem.chatFolder &&
      unreadCount == newItem.unreadCount &&
      isMuted == newItem.isMuted &&
      isSelected == newItem.isSelected
  }
}
