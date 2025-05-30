package org.thoughtcrime.securesms.ryan.database

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.signal.core.util.CursorUtil
import org.thoughtcrime.securesms.ryan.components.settings.app.chats.folders.ChatFolderRecord
import org.thoughtcrime.securesms.ryan.conversationlist.model.ConversationFilter
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.testing.SignalDatabaseRule
import org.thoughtcrime.securesms.ryan.util.RemoteConfig
import org.whispersystems.signalservice.api.push.ServiceId.ACI
import java.util.UUID

@Suppress("ClassName")
class ThreadTableTest_pinned {

  @Rule
  @JvmField
  val databaseRule = SignalDatabaseRule()

  private lateinit var recipient: Recipient
  private val allChats: ChatFolderRecord = ChatFolderRecord(folderType = ChatFolderRecord.FolderType.ALL)

  @Before
  fun setUp() {
    mockkStatic(RemoteConfig::class)

    every { RemoteConfig.showChatFolders } returns true

    recipient = Recipient.resolved(SignalDatabase.recipients.getOrInsertFromServiceId(ACI.from(UUID.randomUUID())))
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIDoNotDeleteOrUnpinTheThread() {
    // GIVEN
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    SignalDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    SignalDatabase.messages.deleteMessage(messageId)

    // THEN
    val pinned = SignalDatabase.threads.getPinnedThreadIds()
    assertTrue(threadId in pinned)
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIExpectTheThreadInUnarchivedCount() {
    // GIVEN
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    SignalDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    SignalDatabase.messages.deleteMessage(messageId)

    // THEN
    val unarchivedCount = SignalDatabase.threads.getUnarchivedConversationListCount(ConversationFilter.OFF, allChats)
    assertEquals(1, unarchivedCount)
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIExpectPinnedThreadInUnarchivedList() {
    // GIVEN
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    SignalDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    SignalDatabase.messages.deleteMessage(messageId)

    // THEN
    SignalDatabase.threads.getUnarchivedConversationList(ConversationFilter.OFF, true, 0, 1, allChats).use {
      it.moveToFirst()
      assertEquals(threadId, CursorUtil.requireLong(it, ThreadTable.ID))
    }
  }
}
