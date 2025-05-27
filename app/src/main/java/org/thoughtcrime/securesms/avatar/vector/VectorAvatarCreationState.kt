package org.thoughtcrime.securesms.ryan.avatar.vector

import org.thoughtcrime.securesms.ryan.avatar.Avatar
import org.thoughtcrime.securesms.ryan.avatar.AvatarColorItem
import org.thoughtcrime.securesms.ryan.avatar.Avatars

data class VectorAvatarCreationState(
  val currentAvatar: Avatar.Vector
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
