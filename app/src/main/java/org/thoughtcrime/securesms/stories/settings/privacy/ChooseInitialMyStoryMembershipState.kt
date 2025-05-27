package org.thoughtcrime.securesms.ryan.stories.settings.privacy

import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.stories.settings.my.MyStoryPrivacyState

data class ChooseInitialMyStoryMembershipState(
  val recipientId: RecipientId? = null,
  val privacyState: MyStoryPrivacyState = MyStoryPrivacyState(),
  val allSignalConnectionsCount: Int = 0,
  val hasUserPerformedManualSelection: Boolean = false
)
