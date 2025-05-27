package org.thoughtcrime.securesms.ryan.database.model

import org.thoughtcrime.securesms.ryan.recipients.RecipientId

/** A model for [org.thoughtcrime.securesms.ryan.database.PendingRetryReceiptTable] */
data class PendingRetryReceiptModel(
  val id: Long,
  val author: RecipientId,
  val authorDevice: Int,
  val sentTimestamp: Long,
  val receivedTimestamp: Long,
  val threadId: Long
)
