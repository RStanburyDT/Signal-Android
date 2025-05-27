/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.conversation.clicklisteners

import android.view.View
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.database.model.MessageRecord
import org.thoughtcrime.securesms.ryan.mms.Slide
import org.thoughtcrime.securesms.ryan.mms.SlidesClickedListener
import org.thoughtcrime.securesms.ryan.sms.MessageSender

class ResendClickListener(private val messageRecord: MessageRecord) : SlidesClickedListener {
  override fun onClick(v: View?, slides: MutableList<Slide>?) {
    if (v == null) {
      Log.w(TAG, "Could not resend message, view was null!")
      return
    }

    SignalExecutors.BOUNDED.execute {
      MessageSender.resend(v.context, messageRecord)
    }
  }

  companion object {
    private val TAG = Log.tag(ResendClickListener::class.java)
  }
}
