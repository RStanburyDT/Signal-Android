package org.thoughtcrime.securesms.ryan.stories.settings.group

import org.thoughtcrime.securesms.ryan.recipients.RecipientId

/**
 * Minimum data needed to launch ConversationActivity for a given grou
 */
data class GroupConversationData(
  val groupRecipientId: RecipientId,
  val groupThreadId: Long
)
