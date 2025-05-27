package org.thoughtcrime.securesms.ryan.stories.settings.custom

import org.thoughtcrime.securesms.ryan.database.model.DistributionListRecord

data class PrivateStorySettingsState(
  val privateStory: DistributionListRecord? = null,
  val areRepliesAndReactionsEnabled: Boolean = false,
  val isActionInProgress: Boolean = false
)
