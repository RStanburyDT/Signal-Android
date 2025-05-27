package org.thoughtcrime.securesms.ryan.stories.my

import org.thoughtcrime.securesms.ryan.conversation.ConversationMessage
import org.thoughtcrime.securesms.ryan.database.model.MessageRecord

data class MyStoriesState(
  val distributionSets: List<DistributionSet> = emptyList()
) {

  data class DistributionSet(
    val label: String?,
    val stories: List<DistributionStory>
  )

  data class DistributionStory(
    val message: ConversationMessage,
    val views: Int
  ) {
    val messageRecord: MessageRecord = message.messageRecord
  }
}
