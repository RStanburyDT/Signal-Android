package org.thoughtcrime.securesms.ryan.stories.settings.story

import org.thoughtcrime.securesms.ryan.contacts.paged.ContactSearchData

data class StoriesPrivacySettingsState(
  val areStoriesEnabled: Boolean,
  val areViewReceiptsEnabled: Boolean,
  val isUpdatingEnabledState: Boolean = false,
  val storyContactItems: List<ContactSearchData> = emptyList(),
  val userHasStories: Boolean = false
)
