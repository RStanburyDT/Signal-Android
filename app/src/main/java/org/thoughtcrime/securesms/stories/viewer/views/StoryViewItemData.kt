package org.thoughtcrime.securesms.ryan.stories.viewer.views

import org.thoughtcrime.securesms.ryan.recipients.Recipient

data class StoryViewItemData(
  val recipient: Recipient,
  val timeViewedInMillis: Long
)
