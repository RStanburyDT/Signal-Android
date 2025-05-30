package org.thoughtcrime.securesms.ryan.contacts.paged

import org.thoughtcrime.securesms.ryan.conversationlist.chatfilter.ConversationFilterRequest

/**
 * Simple search state for contacts.
 */
data class ContactSearchState(
  val query: String? = null,
  val conversationFilterRequest: ConversationFilterRequest? = null,
  val expandedSections: Set<ContactSearchConfiguration.SectionKey> = emptySet(),
  val groupStories: Set<ContactSearchData.Story> = emptySet()
)
