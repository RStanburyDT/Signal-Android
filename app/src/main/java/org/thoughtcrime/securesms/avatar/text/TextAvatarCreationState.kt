package org.thoughtcrime.securesms.ryan.avatar.text

import org.thoughtcrime.securesms.ryan.avatar.Avatar
import org.thoughtcrime.securesms.ryan.avatar.AvatarColorItem
import org.thoughtcrime.securesms.ryan.avatar.Avatars

data class TextAvatarCreationState(
  val currentAvatar: Avatar.Text
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
