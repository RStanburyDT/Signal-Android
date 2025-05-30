package org.thoughtcrime.securesms.ryan.longmessage

import android.content.Context
import android.net.Uri
import org.signal.core.util.StreamUtil
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.conversation.ConversationMessage
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.ryan.mms.PartAuthority
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import java.io.IOException

const val TAG = "LongMessageResolver"

fun readFullBody(context: Context, uri: Uri): String {
  try {
    PartAuthority.getAttachmentStream(context, uri).use { stream -> return StreamUtil.readFullyAsString(stream) }
  } catch (e: IOException) {
    Log.w(TAG, "Failed to read full text body.", e)
    return ""
  }
}

fun MmsMessageRecord.resolveBody(context: Context): ConversationMessage {
  val threadRecipient: Recipient = requireNotNull(SignalDatabase.threads.getRecipientForThreadId(threadId))
  val textSlide = slideDeck.textSlide
  val textSlideUri = textSlide?.uri
  return if (textSlide != null && textSlideUri != null) {
    ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, this, readFullBody(context, textSlideUri), threadRecipient)
  } else {
    ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, this, threadRecipient)
  }
}
