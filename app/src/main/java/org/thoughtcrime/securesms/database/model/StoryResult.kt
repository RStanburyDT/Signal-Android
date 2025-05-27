package org.thoughtcrime.securesms.ryan.database.model

import org.thoughtcrime.securesms.ryan.recipients.RecipientId

class StoryResult(
  val recipientId: RecipientId,
  val messageId: Long,
  val messageSentTimestamp: Long,
  val isOutgoing: Boolean
)
