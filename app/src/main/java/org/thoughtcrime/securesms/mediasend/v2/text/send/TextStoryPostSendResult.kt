package org.thoughtcrime.securesms.ryan.mediasend.v2.text.send

import org.thoughtcrime.securesms.ryan.database.model.IdentityRecord

sealed class TextStoryPostSendResult {
  object Success : TextStoryPostSendResult()
  object Failure : TextStoryPostSendResult()
  data class UntrustedRecordsError(val untrustedRecords: List<IdentityRecord>) : TextStoryPostSendResult()
}
