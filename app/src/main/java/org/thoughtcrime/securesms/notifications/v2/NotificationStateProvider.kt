package org.thoughtcrime.securesms.ryan.notifications.v2

import androidx.annotation.WorkerThread
import org.signal.core.util.CursorUtil
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.database.MessageTable
import org.thoughtcrime.securesms.ryan.database.NoSuchMessageException
import org.thoughtcrime.securesms.ryan.database.RecipientTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.MessageId
import org.thoughtcrime.securesms.ryan.database.model.MessageRecord
import org.thoughtcrime.securesms.ryan.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.ryan.database.model.ReactionRecord
import org.thoughtcrime.securesms.ryan.notifications.profiles.NotificationProfile
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.util.isStoryReaction

/**
 * Queries the message databases to determine messages that should be in notifications.
 */
object NotificationStateProvider {

  private val TAG = Log.tag(NotificationStateProvider::class.java)

  @WorkerThread
  fun constructNotificationState(stickyThreads: Map<ConversationId, DefaultMessageNotifier.StickyThread>, notificationProfile: NotificationProfile?): NotificationState {
    val messages: MutableList<NotificationMessage> = mutableListOf()

    SignalDatabase.messages.getMessagesForNotificationState(stickyThreads.values).use { unreadMessages ->
      if (unreadMessages.count == 0) {
        return NotificationState.EMPTY
      }

      MessageTable.mmsReaderFor(unreadMessages).use { reader ->
        var record: MessageRecord? = reader.getNext()
        while (record != null) {
          val threadRecipient: Recipient? = SignalDatabase.threads.getRecipientForThreadId(record.threadId)
          if (threadRecipient != null) {
            val hasUnreadReactions = CursorUtil.requireInt(unreadMessages, MessageTable.REACTIONS_UNREAD) == 1
            val conversationId = ConversationId.fromMessageRecord(record)

            val parentRecord = conversationId.groupStoryId?.let {
              try {
                SignalDatabase.messages.getMessageRecord(it)
              } catch (e: NoSuchMessageException) {
                null
              }
            }

            val hasSelfRepliedToGroupStory = conversationId.groupStoryId?.let {
              SignalDatabase.messages.hasGroupReplyOrReactionInStory(it)
            }

            if (record is MmsMessageRecord) {
              val attachments = SignalDatabase.attachments.getAttachmentsForMessage(record.id)
              if (attachments.isNotEmpty()) {
                record = record.withAttachments(attachments)
              }
            }

            messages += NotificationMessage(
              messageRecord = record,
              reactions = if (hasUnreadReactions) SignalDatabase.reactions.getReactions(MessageId(record.id)) else emptyList(),
              threadRecipient = threadRecipient,
              thread = conversationId,
              stickyThread = stickyThreads.containsKey(conversationId),
              isUnreadMessage = CursorUtil.requireInt(unreadMessages, MessageTable.READ) == 0,
              hasUnreadReactions = hasUnreadReactions,
              lastReactionRead = CursorUtil.requireLong(unreadMessages, MessageTable.REACTIONS_LAST_SEEN),
              isParentStorySentBySelf = parentRecord?.isOutgoing ?: false,
              hasSelfRepliedToStory = hasSelfRepliedToGroupStory ?: false
            )
          }
          try {
            record = reader.getNext()
          } catch (e: IllegalStateException) {
            // XXX Weird SQLCipher bug that's being investigated
            record = null
            Log.w(TAG, "Failed to read next record!", e)
          }
        }
      }
    }

    val conversations: MutableList<NotificationConversation> = mutableListOf()
    val muteFilteredMessages: MutableList<NotificationState.FilteredMessage> = mutableListOf()
    val profileFilteredMessages: MutableList<NotificationState.FilteredMessage> = mutableListOf()

    messages.groupBy { it.thread }
      .forEach { (thread, threadMessages) ->
        var notificationItems: MutableList<NotificationItem> = mutableListOf()

        for (notification: NotificationMessage in threadMessages) {
          when (notification.includeMessage(notificationProfile)) {
            MessageInclusion.INCLUDE -> notificationItems.add(MessageNotification(notification.threadRecipient, notification.messageRecord))
            MessageInclusion.EXCLUDE -> Unit
            MessageInclusion.MUTE_FILTERED -> muteFilteredMessages += NotificationState.FilteredMessage(notification.messageRecord.id, notification.messageRecord.isMms)
            MessageInclusion.PROFILE_FILTERED -> profileFilteredMessages += NotificationState.FilteredMessage(notification.messageRecord.id, notification.messageRecord.isMms)
          }

          if (notification.hasUnreadReactions) {
            notification.reactions.forEach {
              when (notification.includeReaction(it, notificationProfile)) {
                MessageInclusion.INCLUDE -> notificationItems.add(ReactionNotification(notification.threadRecipient, notification.messageRecord, it))
                MessageInclusion.EXCLUDE -> Unit
                MessageInclusion.MUTE_FILTERED -> muteFilteredMessages += NotificationState.FilteredMessage(notification.messageRecord.id, notification.messageRecord.isMms)
                MessageInclusion.PROFILE_FILTERED -> profileFilteredMessages += NotificationState.FilteredMessage(notification.messageRecord.id, notification.messageRecord.isMms)
              }
            }
          }
        }

        notificationItems.sort()
        if (notificationItems.isNotEmpty() && stickyThreads.containsKey(thread) && !notificationItems.last().authorRecipient.isSelf) {
          val indexOfOldestNonSelfMessage: Int = notificationItems.indexOfLast { it.authorRecipient.isSelf } + 1
          notificationItems = notificationItems.slice(indexOfOldestNonSelfMessage..notificationItems.lastIndex).toMutableList()
        }

        if (notificationItems.isNotEmpty()) {
          conversations += NotificationConversation(notificationItems[0].threadRecipient, thread, notificationItems)
        }
      }

    return NotificationState(conversations, muteFilteredMessages, profileFilteredMessages)
  }

  private data class NotificationMessage(
    val messageRecord: MessageRecord,
    val reactions: List<ReactionRecord>,
    val threadRecipient: Recipient,
    val thread: ConversationId,
    val stickyThread: Boolean,
    val isUnreadMessage: Boolean,
    val hasUnreadReactions: Boolean,
    val lastReactionRead: Long,
    val isParentStorySentBySelf: Boolean,
    val hasSelfRepliedToStory: Boolean
  ) {
    private val isGroupStoryReply: Boolean = thread.groupStoryId != null
    private val isUnreadIncoming: Boolean = isUnreadMessage && !messageRecord.isOutgoing && !isGroupStoryReply
    private val isIncomingMissedCall: Boolean = !messageRecord.isOutgoing && (messageRecord.isMissedAudioCall || messageRecord.isMissedVideoCall)

    private val isNotifiableGroupStoryMessage: Boolean =
      isUnreadMessage &&
        !messageRecord.isOutgoing &&
        isGroupStoryReply &&
        (isParentStorySentBySelf || messageRecord.hasSelfMention() || (hasSelfRepliedToStory && !messageRecord.isStoryReaction()))

    fun includeMessage(notificationProfile: NotificationProfile?): MessageInclusion {
      return if (isUnreadIncoming || stickyThread || isNotifiableGroupStoryMessage || isIncomingMissedCall) {
        if (threadRecipient.isMuted && (threadRecipient.isDoNotNotifyMentions || !messageRecord.hasSelfMention())) {
          MessageInclusion.MUTE_FILTERED
        } else if (notificationProfile != null && !notificationProfile.isRecipientAllowed(threadRecipient.id) && !(notificationProfile.allowAllMentions && messageRecord.hasSelfMention())) {
          MessageInclusion.PROFILE_FILTERED
        } else {
          MessageInclusion.INCLUDE
        }
      } else {
        MessageInclusion.EXCLUDE
      }
    }

    fun includeReaction(reaction: ReactionRecord, notificationProfile: NotificationProfile?): MessageInclusion {
      return if (threadRecipient.isMuted) {
        MessageInclusion.MUTE_FILTERED
      } else if (notificationProfile != null && !notificationProfile.isRecipientAllowed(threadRecipient.id)) {
        MessageInclusion.PROFILE_FILTERED
      } else if (reaction.author != Recipient.self().id && messageRecord.isOutgoing && reaction.dateReceived > lastReactionRead) {
        MessageInclusion.INCLUDE
      } else {
        MessageInclusion.EXCLUDE
      }
    }

    private val Recipient.isDoNotNotifyMentions: Boolean
      get() = mentionSetting == RecipientTable.MentionSetting.DO_NOT_NOTIFY
  }

  private enum class MessageInclusion {
    INCLUDE,
    EXCLUDE,
    MUTE_FILTERED,
    PROFILE_FILTERED
  }
}
