package org.thoughtcrime.securesms.ryan.jobmanager.migrations

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.thoughtcrime.securesms.ryan.database.GroupTable
import org.thoughtcrime.securesms.ryan.database.model.GroupRecord
import org.thoughtcrime.securesms.ryan.groups.GroupId
import org.thoughtcrime.securesms.ryan.jobmanager.JobMigration.JobData
import org.thoughtcrime.securesms.ryan.jobmanager.JsonJobData
import org.thoughtcrime.securesms.ryan.jobs.FailingJob
import org.thoughtcrime.securesms.ryan.jobs.SenderKeyDistributionSendJob
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.util.Util
import java.util.Optional

class SenderKeyDistributionSendJobRecipientMigrationTest {
  private val mockDatabase = mockk<GroupTable>(relaxed = true)
  private val testSubject = SenderKeyDistributionSendJobRecipientMigration(mockDatabase)

  @Test
  fun normalMigration() {
    // GIVEN
    val jobData = JobData(
      factoryKey = SenderKeyDistributionSendJob.KEY,
      queueKey = "asdf",
      maxAttempts = -1,
      lifespan = -1,
      data = JsonJobData.Builder()
        .putString("recipient_id", RecipientId.from(1).serialize())
        .putBlobAsString("group_id", GROUP_ID.decodedId)
        .serialize()
    )

    val mockGroup = mockk<GroupRecord> {
      every { recipientId } returns RecipientId.from(2)
    }
    every { mockDatabase.getGroup(GROUP_ID) } returns Optional.of(mockGroup)

    // WHEN
    val result = testSubject.migrate(jobData)
    val data = JsonJobData.deserialize(result.data)

    // THEN
    assertEquals(RecipientId.from(1).serialize(), data.getString("recipient_id"))
    assertEquals(RecipientId.from(2).serialize(), data.getString("thread_recipient_id"))
  }

  @Test
  fun cannotFindGroup() {
    // GIVEN
    val jobData = JobData(
      factoryKey = SenderKeyDistributionSendJob.KEY,
      queueKey = "asdf",
      maxAttempts = -1,
      lifespan = -1,
      data = JsonJobData.Builder()
        .putString("recipient_id", RecipientId.from(1).serialize())
        .putBlobAsString("group_id", GROUP_ID.decodedId)
        .serialize()
    )

    // WHEN
    val result = testSubject.migrate(jobData)

    // THEN
    assertEquals(FailingJob.KEY, result.factoryKey)
  }

  @Test
  fun missingGroupId() {
    // GIVEN
    val jobData = JobData(
      factoryKey = SenderKeyDistributionSendJob.KEY,
      queueKey = "asdf",
      maxAttempts = -1,
      lifespan = -1,
      data = JsonJobData.Builder()
        .putString("recipient_id", RecipientId.from(1).serialize())
        .serialize()
    )

    // WHEN
    val result = testSubject.migrate(jobData)

    // THEN
    assertEquals(FailingJob.KEY, result.factoryKey)
  }

  companion object {
    private val GROUP_ID: GroupId = GroupId.pushOrThrow(Util.getSecretBytes(32))
  }
}
