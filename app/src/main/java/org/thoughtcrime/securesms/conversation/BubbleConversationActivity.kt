package org.thoughtcrime.securesms.ryan.conversation

import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.conversation.v2.ConversationActivity
import org.thoughtcrime.securesms.ryan.util.ViewUtil

/**
 * Activity which encapsulates a conversation for a Bubble window.
 *
 * This activity exists so that we can override some of its manifest parameters
 * without clashing with [ConversationActivity] and provide an API-level
 * independent "is in bubble?" check.
 */
class BubbleConversationActivity : ConversationActivity() {
  override fun onPause() {
    super.onPause()
    ViewUtil.hideKeyboard(this, findViewById(R.id.fragment_container))
  }
}
