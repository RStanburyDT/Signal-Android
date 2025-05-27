package org.thoughtcrime.securesms.ryan.stories.settings.group

import org.thoughtcrime.securesms.ryan.recipients.Recipient

data class GroupStorySettingsState(
  val name: String = "",
  val members: List<Recipient> = emptyList(),
  val removed: Boolean = false
)
