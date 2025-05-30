package org.thoughtcrime.securesms.ryan.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isPresent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.signal.core.util.Hex
import org.signal.libsignal.zkgroup.groups.GroupMasterKey
import org.thoughtcrime.securesms.ryan.database.MessageTable.InsertResult
import org.thoughtcrime.securesms.ryan.database.model.GroupsV2UpdateMessageConverter
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.DecryptedGroupV2Context
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.GV2UpdateDescription
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.addMember
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.addRequestingMember
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.deleteRequestingMember
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.groupChange
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.groupContext
import org.thoughtcrime.securesms.ryan.groups.GroupId
import org.thoughtcrime.securesms.ryan.isAbsent
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.mms.IncomingMessage
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.whispersystems.signalservice.api.push.ServiceId.ACI
import org.whispersystems.signalservice.api.push.ServiceId.PNI
import java.util.UUID

@Suppress("ClassName", "TestFunctionName")
@RunWith(AndroidJUnit4::class)
class SmsDatabaseTest_collapseJoinRequestEventsIfPossible {

  private lateinit var recipients: RecipientTable
  private lateinit var sms: MessageTable

  private val localAci = ACI.from(UUID.randomUUID())
  private val localPni = PNI.from(UUID.randomUUID())

  private var wallClock: Long = 1000

  private lateinit var alice: RecipientId
  private lateinit var bob: RecipientId

  @Before
  fun setUp() {
    recipients = SignalDatabase.recipients
    sms = SignalDatabase.messages

    SignalStore.account.setAci(localAci)
    SignalStore.account.setPni(localPni)

    alice = recipients.getOrInsertFromServiceId(aliceServiceId)
    bob = recipients.getOrInsertFromServiceId(bobServiceId)
  }

  /**
   * Do nothing if no previous messages.
   */
  @Test
  fun noPreviousMessage() {
    val result = sms.collapseJoinRequestEventsIfPossible(
      1,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is null when not collapsing").isAbsent()
  }

  /**
   * Do nothing if previous message is text.
   */
  @Test
  fun previousTextMesssage() {
    val threadId = sms.insertMessageInbox(smsMessage(sender = alice, body = "What up")).get().threadId

    val result = sms.collapseJoinRequestEventsIfPossible(
      threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is null when not collapsing").isAbsent()
  }

  /**
   * Do nothing if previous is unrelated group change.
   */
  @Test
  fun previousUnrelatedGroupChange() {
    val threadId = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            addMember(bobServiceId)
          }
        }
      )
    ).get().threadId

    val result = sms.collapseJoinRequestEventsIfPossible(
      threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is null when not collapsing").isAbsent()
  }

  /**
   * Do nothing if previous join request is from a different recipient.
   */
  @Test
  fun previousJoinRequestFromADifferentRecipient() {
    val threadId = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = bob,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = bobServiceId) {
            deleteRequestingMember(bobServiceId)
          }
        }
      )
    ).get().threadId

    val result = sms.collapseJoinRequestEventsIfPossible(
      threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is null when not collapsing").isAbsent()
  }

  /**
   * Collapse if previous is join request from same.
   */
  @Test
  fun previousJoinRequestCollapse() {
    val latestMessage: MessageTable.InsertResult = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            addRequestingMember(aliceServiceId)
          }
        }
      )
    ).get()

    val result = sms.collapseJoinRequestEventsIfPossible(
      latestMessage.threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is not null when collapsing")
      .isPresent()
      .given { result: InsertResult ->
        assertThat(result.messageId, "result message id should be same as latest message")
          .isEqualTo(latestMessage.messageId)
      }
  }

  /**
   * Collapse if previous is join request from same, and leave second previous alone if text.
   */
  @Test
  fun previousJoinThenTextCollapse() {
    val secondLatestMessage = sms.insertMessageInbox(smsMessage(sender = alice, body = "What up")).get()

    val latestMessage: MessageTable.InsertResult = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            addRequestingMember(aliceServiceId)
          }
        }
      )
    ).get()

    assert(secondLatestMessage.threadId == latestMessage.threadId)

    val result = sms.collapseJoinRequestEventsIfPossible(
      latestMessage.threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is not null when collapsing")
      .isPresent()
      .given { result: InsertResult ->
        assertThat(result.messageId, "result message id should be same as latest message")
          .isEqualTo(latestMessage.messageId)
      }
  }

  /**
   * Collapse "twice" is previous is a join request and second previous is already collapsed join/delete from the same recipient.
   */
  @Test
  fun previousCollapseAndJoinRequestDoubleCollapse() {
    val secondLatestMessage: MessageTable.InsertResult = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            addRequestingMember(aliceServiceId)
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    ).get()

    val latestMessage: MessageTable.InsertResult = sms.insertMessageInbox(
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            addRequestingMember(aliceServiceId)
          }
        }
      )
    ).get()

    assert(secondLatestMessage.threadId == latestMessage.threadId)

    val result = sms.collapseJoinRequestEventsIfPossible(
      latestMessage.threadId,
      groupUpdateMessage(
        sender = alice,
        groupContext = groupContext(masterKey = masterKey) {
          change = groupChange(editor = aliceServiceId) {
            deleteRequestingMember(aliceServiceId)
          }
        }
      )
    )

    assertThat(result, "result is not null when collapsing")
      .isPresent()
      .given { result: InsertResult ->
        assertThat(result.messageId, "result message id should be same as second latest message")
          .isEqualTo(secondLatestMessage.messageId)
        assertThat(sms.getMessageRecordOrNull(latestMessage.messageId), "latest message should be deleted").isNull()
      }
  }

  private fun smsMessage(sender: RecipientId, body: String? = ""): IncomingMessage {
    wallClock++
    return IncomingMessage(
      type = MessageType.NORMAL,
      from = sender,
      sentTimeMillis = wallClock,
      serverTimeMillis = wallClock,
      receivedTimeMillis = wallClock,
      body = body,
      groupId = groupId,
      isUnidentified = true
    )
  }

  private fun groupUpdateMessage(sender: RecipientId, groupContext: DecryptedGroupV2Context): IncomingMessage {
    wallClock++

    val updateDescription = GV2UpdateDescription(
      gv2ChangeDescription = groupContext,
      groupChangeUpdate = GroupsV2UpdateMessageConverter.translateDecryptedChangeUpdate(SignalStore.account.getServiceIds(), groupContext)
    )

    return IncomingMessage.groupUpdate(
      from = sender,
      timestamp = wallClock,
      groupId = groupId,
      update = updateDescription,
      isGroupAdd = false,
      serverGuid = null
    )
  }

  companion object {
    private val aliceServiceId: ACI = ACI.from(UUID.fromString("3436efbe-5a76-47fa-a98a-7e72c948a82e"))
    private val bobServiceId: ACI = ACI.from(UUID.fromString("8de7f691-0b60-4a68-9cd9-ed2f8453f9ed"))

    private val masterKey = GroupMasterKey(Hex.fromStringCondensed("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
    private val groupId = GroupId.v2(masterKey)
  }
}
