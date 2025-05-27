package org.thoughtcrime.securesms.ryan.components.settings.conversation

import org.thoughtcrime.securesms.ryan.util.DynamicNoActionBarTheme
import org.thoughtcrime.securesms.ryan.util.DynamicTheme

class CallInfoActivity : ConversationSettingsActivity(), ConversationSettingsFragment.Callback {

  override val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()
}
