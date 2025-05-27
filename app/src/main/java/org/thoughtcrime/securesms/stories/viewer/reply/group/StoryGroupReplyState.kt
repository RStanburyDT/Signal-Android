package org.thoughtcrime.securesms.ryan.stories.viewer.reply.group

import org.thoughtcrime.securesms.ryan.conversation.colors.NameColor
import org.thoughtcrime.securesms.ryan.recipients.RecipientId

data class StoryGroupReplyState(
  val threadId: Long = 0L,
  val replies: List<ReplyBody> = emptyList(),
  val nameColors: Map<RecipientId, NameColor> = emptyMap(),
  val loadState: LoadState = LoadState.INIT
) {
  val noReplies: Boolean = replies.isEmpty()

  enum class LoadState {
    INIT,
    READY
  }
}
