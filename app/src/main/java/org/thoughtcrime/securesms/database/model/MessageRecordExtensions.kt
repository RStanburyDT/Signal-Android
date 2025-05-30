/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.database.model

import org.thoughtcrime.securesms.ryan.attachments.DatabaseAttachment
import org.thoughtcrime.securesms.ryan.database.CallTable
import org.thoughtcrime.securesms.ryan.payments.Payment

fun MessageRecord.withReactions(reactions: List<ReactionRecord>): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withReactions(reactions)
  } else {
    this
  }
}

fun MessageRecord.withAttachments(attachments: List<DatabaseAttachment>): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withAttachments(attachments)
  } else {
    this
  }
}
fun MessageRecord.withPayment(payment: Payment): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withPayment(payment)
  } else {
    this
  }
}

fun MessageRecord.withCall(call: CallTable.Call): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withCall(call)
  } else {
    this
  }
}
