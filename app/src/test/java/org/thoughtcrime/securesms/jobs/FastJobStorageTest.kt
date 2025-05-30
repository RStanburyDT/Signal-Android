package org.thoughtcrime.securesms.ryan.jobs

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.thoughtcrime.securesms.ryan.database.JobDatabase
import org.thoughtcrime.securesms.ryan.jobmanager.Job
import org.thoughtcrime.securesms.ryan.jobmanager.persistence.ConstraintSpec
import org.thoughtcrime.securesms.ryan.jobmanager.persistence.DependencySpec
import org.thoughtcrime.securesms.ryan.jobmanager.persistence.FullSpec
import org.thoughtcrime.securesms.ryan.jobmanager.persistence.JobSpec
import org.thoughtcrime.securesms.ryan.testutil.TestHelpers
import java.nio.charset.Charset

class FastJobStorageTest {

  companion object {
    val NO_PREDICATE: (MinimalJobSpec) -> Boolean = { true }
  }

  @Test
  fun `init - all stored data available`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    DataSet1.assertJobsMatch(subject.debugGetJobSpecs(1000))
    DataSet1.assertConstraintsMatch(subject.debugGetConstraintSpecs(1000))
    DataSet1.assertDependenciesMatch(subject.debugGetAllDependencySpecs())
  }

  @Test
  fun `init - removes circular dependencies`() {
    val subject = FastJobStorage(mockDatabase(DataSetCircularDependency.FULL_SPECS))
    subject.init()

    DataSetCircularDependency.assertJobsMatch(subject.debugGetJobSpecs(1000))
    DataSetCircularDependency.assertConstraintsMatch(subject.debugGetConstraintSpecs(1000))
    DataSetCircularDependency.assertDependenciesMatch(subject.debugGetAllDependencySpecs())
  }

  @Test
  fun `insertJobs - writes to database`() {
    val database = mockDatabase()
    val subject = FastJobStorage(database)

    subject.insertJobs(DataSet1.FULL_SPECS)

    verify { database.insertJobs(DataSet1.FULL_SPECS) }
  }

  @Test
  fun `insertJobs - memory-only job does not write to database`() {
    val database = mockDatabase()
    val subject = FastJobStorage(database)

    subject.insertJobs(DataSetMemory.FULL_SPECS)

    verify(exactly = 0) { database.insertJobs(DataSet1.FULL_SPECS) }
  }

  @Test
  fun `insertJobs - data can be found`() {
    val subject = FastJobStorage(mockDatabase())
    subject.insertJobs(DataSet1.FULL_SPECS)
    DataSet1.assertJobsMatch(subject.debugGetJobSpecs(1000))
    DataSet1.assertConstraintsMatch(subject.debugGetConstraintSpecs(1000))
    DataSet1.assertDependenciesMatch(subject.debugGetAllDependencySpecs())
  }

  @Test
  fun `insertJobs - individual job can be found`() {
    val subject = FastJobStorage(mockDatabase())
    subject.insertJobs(DataSet1.FULL_SPECS)

    assertThat(subject.getJobSpec(DataSet1.JOB_1.id)).isEqualTo(DataSet1.JOB_1)
    assertThat(subject.getJobSpec(DataSet1.JOB_2.id)).isEqualTo(DataSet1.JOB_2)
  }

  @Test
  fun `updateAllJobsToBePending - writes to database`() {
    val database = mockDatabase()
    val subject = FastJobStorage(database)
    subject.updateAllJobsToBePending()
    verify { database.updateAllJobsToBePending() }
  }

  @Test
  fun `updateAllJobsToBePending - all are pending`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", isRunning = true), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", isRunning = true), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()
    subject.updateAllJobsToBePending()

    assertThat(subject.getJobSpec("1")!!.isRunning).isEqualTo(false)
    assertThat(subject.getJobSpec("2")!!.isRunning).isEqualTo(false)
  }

  @Test
  fun `updateJobs - writes to database`() {
    val database = mockDatabase(DataSet1.FULL_SPECS)
    val jobs = listOf(jobSpec(id = "id1", factoryKey = "f1"))

    val subject = FastJobStorage(database)
    subject.init()
    subject.updateJobs(jobs)

    verify { database.updateJobs(jobs) }
  }

  @Test
  fun `updateJobs - memory-only job does not write to database`() {
    val database = mockDatabase(DataSetMemory.FULL_SPECS)
    val jobs = listOf(jobSpec(id = "id1", factoryKey = "f1"))

    val subject = FastJobStorage(database)
    subject.init()
    subject.updateJobs(jobs)

    verify(exactly = 0) { database.updateJobs(jobs) }
  }

  @Test
  fun `updateJobs - updates all fields`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1"), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2"), emptyList(), emptyList())
    val fullSpec3 = FullSpec(jobSpec(id = "3", factoryKey = "f3"), emptyList(), emptyList())

    val update1 = jobSpec(
      id = "1",
      factoryKey = "g1",
      queueKey = "q1",
      createTime = 2,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 2,
      runAttempt = 2,
      maxAttempts = 2,
      lifespan = 2,
      serializedData = "abc".toByteArray(),
      serializedInputData = null,
      isRunning = true,
      isMemoryOnly = false
    )
    val update2 = jobSpec(
      id = "2",
      factoryKey = "g2",
      queueKey = "q2",
      createTime = 3,
      lastRunAttemptTime = 3,
      nextBackoffInterval = 3,
      runAttempt = 3,
      maxAttempts = 3,
      lifespan = 3,
      serializedData = "def".toByteArray(),
      serializedInputData = "ghi".toByteArray(),
      isRunning = true,
      isMemoryOnly = false
    )

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2, fullSpec3)))
    subject.init()
    subject.updateJobs(listOf(update1, update2))

    assertThat(subject.getJobSpec("1")).isEqualTo(update1)
    assertThat(subject.getJobSpec("2")).isEqualTo(update2)
    assertThat(subject.getJobSpec("3")).isEqualTo(fullSpec3.jobSpec)
  }

  @Test
  fun `transformJobs - writes to database`() {
    val database = mockDatabase(DataSet1.FULL_SPECS)

    val subject = FastJobStorage(database)
    subject.init()
    val transformer: (JobSpec) -> JobSpec = { it }
    subject.transformJobs(transformer)

    verify { database.transformJobs(transformer) }
  }

  @Test
  fun `transformJobs - updates all fields`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1"), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2"), emptyList(), emptyList())
    val fullSpec3 = FullSpec(jobSpec(id = "3", factoryKey = "f3"), emptyList(), emptyList())

    val update1 = jobSpec(
      id = "1",
      factoryKey = "g1",
      queueKey = "q1",
      createTime = 2,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 2,
      runAttempt = 2,
      maxAttempts = 2,
      lifespan = 2,
      serializedData = "abc".toByteArray(),
      serializedInputData = null,
      isRunning = true,
      isMemoryOnly = false
    )
    val update2 = jobSpec(
      id = "2",
      factoryKey = "g2",
      queueKey = "q2",
      createTime = 3,
      lastRunAttemptTime = 3,
      nextBackoffInterval = 3,
      runAttempt = 3,
      maxAttempts = 3,
      lifespan = 3,
      serializedData = "def".toByteArray(),
      serializedInputData = "ghi".toByteArray(),
      isRunning = true,
      isMemoryOnly = false
    )

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2, fullSpec3)))
    subject.init()

    subject.transformJobs {
      when (it.id) {
        "1" -> update1
        "2" -> update2
        else -> it
      }
    }

    assertThat(subject.getJobSpec("1")).isEqualTo(update1)
    assertThat(subject.getJobSpec("2")).isEqualTo(update2)
    assertThat(subject.getJobSpec("3")).isEqualTo(fullSpec3.jobSpec)
  }

  @Test
  fun `markJobAsRunning - writes to database`() {
    val database = mockDatabase(DataSet1.FULL_SPECS)

    val subject = FastJobStorage(database)
    subject.init()

    subject.markJobAsRunning(id = "id1", currentTime = 42)

    verify { database.markJobAsRunning(id = "id1", currentTime = 42) }
  }

  @Test
  fun `markJobAsRunning - state updated`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    subject.markJobAsRunning(id = DataSet1.JOB_1.id, currentTime = 42)

    assertThat(subject.getJobSpec(DataSet1.JOB_1.id)!!.isRunning).isEqualTo(true)
    assertThat(subject.getJobSpec(DataSet1.JOB_1.id)!!.lastRunAttemptTime).isEqualTo(42)
  }

  @Test
  fun `updateJobAfterRetry - writes to database`() {
    val database = mockDatabase(DataSet1.FULL_SPECS)

    val subject = FastJobStorage(database)
    subject.init()

    subject.updateJobAfterRetry(
      id = "id1",
      currentTime = 0,
      runAttempt = 1,
      nextBackoffInterval = 10,
      serializedData = "a".toByteArray()
    )

    verify { database.updateJobAfterRetry(id = "id1", currentTime = 0, runAttempt = 1, nextBackoffInterval = 10, serializedData = "a".toByteArray()) }
  }

  @Test
  fun `updateJobAfterRetry - memory-only job does not write to database`() {
    val database = mockDatabase(DataSetMemory.FULL_SPECS)

    val subject = FastJobStorage(database)
    subject.init()

    subject.updateJobAfterRetry(
      id = "id1",
      currentTime = 0,
      runAttempt = 1,
      nextBackoffInterval = 10,
      serializedData = "a".toByteArray()
    )

    verify(exactly = 0) { database.updateJobAfterRetry(id = "id1", currentTime = 0, runAttempt = 1, nextBackoffInterval = 10, serializedData = "a".toByteArray()) }
  }

  @Test
  fun `updateJobAfterRetry - state updated`() {
    val fullSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", isRunning = true, serializedData = "a".toByteArray()), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec)))
    subject.init()

    subject.updateJobAfterRetry(
      id = "1",
      currentTime = 3,
      runAttempt = 2,
      nextBackoffInterval = 10,
      serializedData = "a".toByteArray()
    )

    val job = subject.getJobSpec("1")
    check(job != null)
    assertThat(job.isRunning).isEqualTo(false)
    assertThat(job.lastRunAttemptTime).isEqualTo(3)
    assertThat(job.runAttempt).isEqualTo(2)
    assertThat(job.nextBackoffInterval).isEqualTo(10)
    assertThat(job.serializedData!!.toString(Charset.defaultCharset())).isEqualTo("a")
  }

  @Test
  fun `getNextEligibleJob - none when earlier item in queue is running`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", isRunning = true), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q"), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(1, NO_PREDICATE)).isEqualTo(null)
  }

  @Test
  fun `getNextEligibleJob - none when all jobs are running`() {
    val fullSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", isRunning = true), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isEqualTo(null)
  }

  @Test
  fun `getNextEligibleJob - none when next run time is after current time`() {
    val currentTime = 1L
    val fullSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", lastRunAttemptTime = 0, nextBackoffInterval = 10), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec)))
    subject.init()

    assertThat(subject.getNextEligibleJob(currentTime, NO_PREDICATE)).isEqualTo(null)
  }

  @Test
  fun `getNextEligibleJob - none when dependent on another job`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", isRunning = true), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2"), emptyList(), listOf(DependencySpec("2", "1", false)))

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(0, NO_PREDICATE)).isEqualTo(null)
  }

  @Test
  fun `getNextEligibleJob - single eligible job`() {
    val fullSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q"), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isEqualTo(fullSpec.jobSpec)
  }

  @Test
  fun `getNextEligibleJob - multiple eligible jobs`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1"), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2"), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isEqualTo(fullSpec1.jobSpec)
    subject.deleteJob(fullSpec1.jobSpec.id)

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isEqualTo(fullSpec2.jobSpec)
  }

  @Test
  fun `getNextEligibleJob - single eligible job in mixed list`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", isRunning = true), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2"), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec2.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - first item in queue`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q"), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q"), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec1.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - first item in queue with global priority`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", createTime = 1, globalPriority = Job.Parameters.PRIORITY_LOW), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q", createTime = 2, globalPriority = Job.Parameters.PRIORITY_HIGH), emptyList(), emptyList())
    val fullSpec3 = FullSpec(jobSpec(id = "3", factoryKey = "f3", queueKey = "q", createTime = 3, globalPriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2, fullSpec3)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec2.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - complex global priority`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q1", createTime = 1, globalPriority = Job.Parameters.PRIORITY_LOW, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q1", createTime = 2, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec3 = FullSpec(jobSpec(id = "3", factoryKey = "f3", queueKey = "q2", createTime = 3, globalPriority = Job.Parameters.PRIORITY_DEFAULT, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec4 = FullSpec(jobSpec(id = "4", factoryKey = "f4", queueKey = "q2", createTime = 4, globalPriority = Job.Parameters.PRIORITY_LOW, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec5 = FullSpec(jobSpec(id = "5", factoryKey = "f5", queueKey = "q3", createTime = 5, globalPriority = Job.Parameters.PRIORITY_DEFAULT, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec6 = FullSpec(jobSpec(id = "6", factoryKey = "f6", queueKey = "q3", createTime = 6, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec7 = FullSpec(jobSpec(id = "7", factoryKey = "f7", queueKey = "q4", createTime = 7, globalPriority = Job.Parameters.PRIORITY_LOW, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec8 = FullSpec(jobSpec(id = "8", factoryKey = "f8", queueKey = null, createTime = 8, globalPriority = Job.Parameters.PRIORITY_LOW, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec9 = FullSpec(jobSpec(id = "9", factoryKey = "f9", queueKey = null, createTime = 9, globalPriority = Job.Parameters.PRIORITY_DEFAULT, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec10 = FullSpec(jobSpec(id = "10", factoryKey = "f10", queueKey = "q5", createTime = 10, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())
    val fullSpec11 = FullSpec(jobSpec(id = "11", factoryKey = "f11", queueKey = "q5", createTime = 11, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_HIGH), emptyList(), emptyList())
    val fullSpec12 = FullSpec(jobSpec(id = "12", factoryKey = "f12", queueKey = "q5", createTime = 12, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_LOW), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2, fullSpec3, fullSpec4, fullSpec5, fullSpec6, fullSpec7, fullSpec8, fullSpec9, fullSpec10, fullSpec11, fullSpec12)))
    subject.init()

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec2.jobSpec)
    subject.deleteJob(fullSpec2.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec6.jobSpec)
    subject.deleteJob(fullSpec6.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec11.jobSpec)
    subject.deleteJob(fullSpec11.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec10.jobSpec)
    subject.deleteJob(fullSpec10.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec12.jobSpec)
    subject.deleteJob(fullSpec12.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec3.jobSpec)
    subject.deleteJob(fullSpec3.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec5.jobSpec)
    subject.deleteJob(fullSpec5.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec9.jobSpec)
    subject.deleteJob(fullSpec9.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec1.jobSpec)
    subject.deleteJob(fullSpec1.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec4.jobSpec)
    subject.deleteJob(fullSpec4.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec7.jobSpec)
    subject.deleteJob(fullSpec7.jobSpec.id)

    assertThat(subject.getNextEligibleJob(15, NO_PREDICATE)).isEqualTo(fullSpec8.jobSpec)
  }

  @Test
  fun `getNextEligibleJob - first item in queue with queue priority`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", createTime = 1, queuePriority = Job.Parameters.PRIORITY_LOW), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q", createTime = 2, queuePriority = Job.Parameters.PRIORITY_HIGH), emptyList(), emptyList())
    val fullSpec3 = FullSpec(jobSpec(id = "3", factoryKey = "f3", queueKey = "q", createTime = 3, queuePriority = Job.Parameters.PRIORITY_DEFAULT), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2, fullSpec3)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec2.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - global priority beats queue priority`() {
    val fullSpec1 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q", createTime = 2, globalPriority = Job.Parameters.PRIORITY_DEFAULT, queuePriority = Job.Parameters.PRIORITY_HIGH), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", createTime = 1, globalPriority = Job.Parameters.PRIORITY_HIGH, queuePriority = Job.Parameters.PRIORITY_LOW), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec2.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - lastRunAttemptTime in the future runs right away`() {
    val currentTime = 10L

    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", lastRunAttemptTime = 100, nextBackoffInterval = 5), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1)))
    subject.init()

    val job = subject.getNextEligibleJob(currentTime, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(fullSpec1.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - migration job takes precedence`() {
    val plainSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", createTime = 0), emptyList(), emptyList())
    val migrationSpec = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 5), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(plainSpec, migrationSpec)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(migrationSpec.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - running migration blocks normal jobs`() {
    val plainSpec = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q", createTime = 0), emptyList(), emptyList())
    val migrationSpec = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 5, isRunning = true), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(plainSpec, migrationSpec)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isNull()
  }

  @Test
  fun `getNextEligibleJob - running migration blocks later migration jobs`() {
    val migrationSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 0, isRunning = true), emptyList(), emptyList())
    val migrationSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 5), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(migrationSpec1, migrationSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isNull()
  }

  @Test
  fun `getNextEligibleJob - only return first eligible migration job`() {
    val migrationSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 0), emptyList(), emptyList())
    val migrationSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 5), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(migrationSpec1, migrationSpec2)))
    subject.init()

    val job = subject.getNextEligibleJob(10, NO_PREDICATE)
    assertThat(job).isNotNull()
      .prop(JobSpec::id)
      .isEqualTo(migrationSpec1.jobSpec.id)
  }

  @Test
  fun `getNextEligibleJob - migration job that isn't scheduled to run yet blocks later migration jobs`() {
    val currentTime = 10L

    val migrationSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 0, lastRunAttemptTime = 0, nextBackoffInterval = 999), emptyList(), emptyList())
    val migrationSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = Job.Parameters.MIGRATION_QUEUE_KEY, createTime = 5, lastRunAttemptTime = 0, nextBackoffInterval = 0), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(migrationSpec1, migrationSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(currentTime, NO_PREDICATE)).isNull()
  }

  @Test
  fun `getNextEligibleJob - after deleted, no longer is in eligible list`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)
    subject.deleteJob(DataSet1.JOB_1.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isNotEqualTo(DataSet1.JOB_1)
  }

  @Test
  fun `getNextEligibleJob - after deleted, next item in queue is eligible`() {
    // Two jobs in the same queue but with different create times
    val firstJob = DataSet1.JOB_1
    val secondJob = DataSet1.JOB_1.copy(id = "id2", createTime = 2)
    val subject = FastJobStorage(
      mockDatabase(
        fullSpecs = listOf(
          FullSpec(jobSpec = firstJob, constraintSpecs = emptyList(), dependencySpecs = emptyList()),
          FullSpec(jobSpec = secondJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())
        )
      )
    )
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(firstJob)
    subject.deleteJob(firstJob.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(secondJob)
  }

  @Test
  fun `getNextEligibleJob - after deleted, next item in queue is eligible, with global priorities`() {
    // Two jobs in the same queue but with different create times
    val firstJob = DataSet1.JOB_1
    val secondJob = DataSet1.JOB_1.copy(id = "id2", createTime = 2, globalPriority = Job.Parameters.PRIORITY_HIGH)
    val thirdJob = DataSet1.JOB_1.copy(id = "id3", createTime = 3, globalPriority = Job.Parameters.PRIORITY_HIGH)
    val subject = FastJobStorage(
      mockDatabase(
        fullSpecs = listOf(
          FullSpec(jobSpec = firstJob, constraintSpecs = emptyList(), dependencySpecs = emptyList()),
          FullSpec(jobSpec = secondJob, constraintSpecs = emptyList(), dependencySpecs = emptyList()),
          FullSpec(jobSpec = thirdJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())
        )
      )
    )
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(secondJob)
    subject.deleteJob(secondJob.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(thirdJob)
    subject.deleteJob(thirdJob.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(firstJob)
    subject.deleteJob(firstJob.id)
  }

  @Test
  fun `getNextEligibleJob - after deleted, next item in queue is eligible, with queue priorities`() {
    // Two jobs in the same queue but with different create times
    val firstJob = DataSet1.JOB_1
    val secondJob = DataSet1.JOB_1.copy(id = "id2", createTime = 2, queuePriority = Job.Parameters.PRIORITY_HIGH)
    val thirdJob = DataSet1.JOB_1.copy(id = "id3", createTime = 3, queuePriority = Job.Parameters.PRIORITY_HIGH)
    val subject = FastJobStorage(
      mockDatabase(
        fullSpecs = listOf(
          FullSpec(jobSpec = firstJob, constraintSpecs = emptyList(), dependencySpecs = emptyList()),
          FullSpec(jobSpec = secondJob, constraintSpecs = emptyList(), dependencySpecs = emptyList()),
          FullSpec(jobSpec = thirdJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())
        )
      )
    )
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(secondJob)
    subject.deleteJob(secondJob.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(thirdJob)
    subject.deleteJob(thirdJob.id)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(firstJob)
    subject.deleteJob(firstJob.id)
  }

  @Test
  fun `getNextEligibleJob - after marked running, no longer is in eligible list`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)
    subject.markJobAsRunning(DataSet1.JOB_1.id, 1)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isNotEqualTo(DataSet1.JOB_1)
  }

  @Test
  fun `getNextEligibleJob - after updateJobAfterRetry to be invalid, no longer is in eligible list`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)
    subject.updateJobAfterRetry(DataSet1.JOB_1.id, 1, 1000, 1_000_000, null)

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isNotEqualTo(DataSet1.JOB_1)
  }

  @Test
  fun `getNextEligibleJob - after invalid then marked pending, is in eligible list`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    subject.markJobAsRunning(DataSet1.JOB_1.id, 1)
    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isNotEqualTo(DataSet1.JOB_1)

    subject.updateAllJobsToBePending()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)?.id).isEqualTo(DataSet1.JOB_1.id) // The last run attempt time changes, so some fields will be different
  }

  @Test
  fun `getNextEligibleJob - after updateJobs to be invalid, no longer is in eligible list`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)
    subject.updateJobs(listOf(DataSet1.JOB_1.copy(isRunning = true)))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isNotEqualTo(DataSet1.JOB_1)
  }

  @Test
  fun `getNextEligibleJob - newly-inserted higher-global-priority job in queue replaces old`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)

    val higherPriorityJob = DataSet1.JOB_1.copy(id = "id-bigboi", globalPriority = Job.Parameters.PRIORITY_HIGH)
    subject.insertJobs(listOf(FullSpec(jobSpec = higherPriorityJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(higherPriorityJob)
  }

  @Test
  fun `getNextEligibleJob - newly-inserted higher-queue-priority job in queue replaces old`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)

    val higherPriorityJob = DataSet1.JOB_1.copy(id = "id-bigboi", queuePriority = Job.Parameters.PRIORITY_HIGH)
    subject.insertJobs(listOf(FullSpec(jobSpec = higherPriorityJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(higherPriorityJob)
  }

  @Test
  fun `getNextEligibleJob - updating job to have a higher global priority replaces lower priority in queue`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val lowerPriorityJob = DataSet1.JOB_1.copy(id = "id-bigboi", globalPriority = Job.Parameters.PRIORITY_LOW)
    subject.insertJobs(listOf(FullSpec(jobSpec = lowerPriorityJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)

    val higherPriorityJob = lowerPriorityJob.copy(globalPriority = Job.Parameters.PRIORITY_HIGH)
    subject.updateJobs(listOf(higherPriorityJob))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(higherPriorityJob)
  }

  @Test
  fun `getNextEligibleJob - updating job to have a queue priority replaces lower priority in queue`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val lowerPriorityJob = DataSet1.JOB_1.copy(id = "id-bigboi", queuePriority = Job.Parameters.PRIORITY_LOW)
    subject.insertJobs(listOf(FullSpec(jobSpec = lowerPriorityJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)

    val higherPriorityJob = lowerPriorityJob.copy(queuePriority = Job.Parameters.PRIORITY_HIGH)
    subject.updateJobs(listOf(higherPriorityJob))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(higherPriorityJob)
  }

  @Test
  fun `getNextEligibleJob - updating job to have an older createTime replaces newer in queue`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val newerJob = DataSet1.JOB_1.copy(id = "id-bigboi", createTime = 1000)
    subject.insertJobs(listOf(FullSpec(jobSpec = newerJob, constraintSpecs = emptyList(), dependencySpecs = emptyList())))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(DataSet1.JOB_1)

    val olderJob = newerJob.copy(createTime = 0)
    subject.updateJobs(listOf(olderJob))

    assertThat(subject.getNextEligibleJob(100, NO_PREDICATE)).isEqualTo(olderJob)
  }

  @Test
  fun `getNextEligibleJob - jobs with initial delay will not run until after the delay`() {
    val fullSpec1 = FullSpec(jobSpec(id = "1", factoryKey = "f1", queueKey = "q1", createTime = 1, initialDelay = 10), emptyList(), emptyList())
    val fullSpec2 = FullSpec(jobSpec(id = "2", factoryKey = "f2", queueKey = "q2", createTime = 2, initialDelay = 0), emptyList(), emptyList())

    val subject = FastJobStorage(mockDatabase(listOf(fullSpec1, fullSpec2)))
    subject.init()

    assertThat(subject.getNextEligibleJob(10, NO_PREDICATE)).isEqualTo(fullSpec2.jobSpec)
    subject.deleteJob(fullSpec2.jobSpec.id)

    assertThat(subject.getNextEligibleJob(20, NO_PREDICATE)).isEqualTo(fullSpec1.jobSpec)
  }

  @Test
  fun `deleteJobs - writes to database`() {
    val database = mockDatabase(DataSet1.FULL_SPECS)
    val ids: List<String> = listOf("id1", "id2")

    val subject = FastJobStorage(database)
    subject.init()

    subject.deleteJobs(ids)

    verify { database.deleteJobs(ids) }
  }

  @Test
  fun `deleteJobs - memory-only job does not write to database`() {
    val database = mockDatabase(DataSetMemory.FULL_SPECS)
    val ids = listOf("id1")

    val subject = FastJobStorage(database)
    subject.init()

    subject.deleteJobs(ids)

    verify(exactly = 0) { database.deleteJobs(ids) }
  }

  @Test
  fun `deleteJobs - deletes all relevant pieces`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    subject.deleteJobs(listOf("id1"))

    val jobs = subject.debugGetJobSpecs(1000)
    val constraints = subject.debugGetConstraintSpecs(1000)
    val dependencies = subject.debugGetAllDependencySpecs()

    assertThat(jobs.size).isEqualTo(2)
    assertThat(jobs[0]).isEqualTo(DataSet1.JOB_2)
    assertThat(jobs[1]).isEqualTo(DataSet1.JOB_3)
    assertThat(constraints.size).isEqualTo(1)
    assertThat(constraints[0]).isEqualTo(DataSet1.CONSTRAINT_2)
    assertThat(dependencies.size).isEqualTo(1)
    assertThat(subject.getJobSpec("id1")).isEqualTo(null)
  }

  @Test
  fun `getDependencySpecsThatDependOnJob - start of chain`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val result = subject.getDependencySpecsThatDependOnJob("id1")
    assertThat(result.size).isEqualTo(2)
    assertThat(result[0]).isEqualTo(DataSet1.DEPENDENCY_2)
    assertThat(result[1]).isEqualTo(DataSet1.DEPENDENCY_3)
  }

  @Test
  fun `getDependencySpecsThatDependOnJob - mid-chain`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val result = subject.getDependencySpecsThatDependOnJob("id2")
    assertThat(result.size).isEqualTo(1)
    assertThat(result[0]).isEqualTo(DataSet1.DEPENDENCY_3)
  }

  @Test
  fun `getDependencySpecsThatDependOnJob - end of chain`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val result = subject.getDependencySpecsThatDependOnJob("id3")
    assertThat(result.size).isEqualTo(0)
  }

  @Test
  fun `getJobsInQueue - empty`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val result = subject.getJobsInQueue("x")
    assertThat(result.size).isEqualTo(0)
  }

  @Test
  fun `getJobsInQueue - single job`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    val result = subject.getJobsInQueue("q1")
    assertThat(result.size).isEqualTo(1)
    assertThat(result[0].id).isEqualTo("id1")
  }

  @Test
  fun `getJobCountForFactory - general`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getJobCountForFactory("f1")).isEqualTo(1)
    assertThat(subject.getJobCountForFactory("does-not-exist")).isEqualTo(0)
  }

  @Test
  fun `getJobCountForFactoryAndQueue - general`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.getJobCountForFactoryAndQueue("f1", "q1")).isEqualTo(1)
    assertThat(subject.getJobCountForFactoryAndQueue("f2", "q1")).isEqualTo(0)
    assertThat(subject.getJobCountForFactoryAndQueue("f1", "does-not-exist")).isEqualTo(0)
  }

  @Test
  fun `areQueuesEmpty - all non-empty`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.areQueuesEmpty(TestHelpers.setOf("q1"))).isEqualTo(false)
    assertThat(subject.areQueuesEmpty(TestHelpers.setOf("q1", "q2"))).isEqualTo(false)
  }

  @Test
  fun `areQueuesEmpty - mixed empty`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.areQueuesEmpty(TestHelpers.setOf("q1", "q5"))).isEqualTo(false)
  }

  @Test
  fun `areQueuesEmpty - queue does not exist`() {
    val subject = FastJobStorage(mockDatabase(DataSet1.FULL_SPECS))
    subject.init()

    assertThat(subject.areQueuesEmpty(TestHelpers.setOf("q4"))).isEqualTo(true)
    assertThat(subject.areQueuesEmpty(TestHelpers.setOf("q4", "q5"))).isEqualTo(true)
  }

  private fun mockDatabase(fullSpecs: List<FullSpec> = emptyList()): JobDatabase {
    val jobs = fullSpecs.map { it.jobSpec }.toMutableList()
    val constraints = fullSpecs.map { it.constraintSpecs }.flatten().toMutableList()
    val dependencies = fullSpecs.map { it.dependencySpecs }.flatten().toMutableList()

    val mock = mockk<JobDatabase>(relaxed = true)
    every { mock.getJobSpecs(any()) } returns jobs
    every { mock.getAllMinimalJobSpecs() } returns jobs.map { it.toMinimalJobSpec() }
    every { mock.getConstraintSpecs(any()) } returns constraints
    every { mock.getAllDependencySpecs() } returns dependencies
    every { mock.getConstraintSpecsForJobs(any()) } returns constraints
    every { mock.getJobSpec(any()) } answers { jobs.first { it.id == firstArg() } }
    every { mock.insertJobs(any()) } answers {
      val inserts: List<FullSpec> = firstArg()
      for (insert in inserts) {
        jobs += insert.jobSpec
        constraints += insert.constraintSpecs
        dependencies += insert.dependencySpecs
      }
    }
    every { mock.deleteJobs(any()) } answers {
      val ids: List<String> = firstArg()
      jobs.removeIf { ids.contains(it.id) }
      constraints.removeIf { ids.contains(it.jobSpecId) }
      dependencies.removeIf { ids.contains(it.jobId) || ids.contains(it.dependsOnJobId) }
    }
    every { mock.updateJobs(any()) } answers {
      val updates: List<JobSpec> = firstArg()
      for (update in updates) {
        jobs.removeIf { it.id == update.id }
        jobs += update
      }
    }
    every { mock.transformJobs(any()) } answers {
      val transformer: (JobSpec) -> JobSpec = firstArg()
      val iterator = jobs.listIterator()
      val out = mutableListOf<JobSpec>()
      while (iterator.hasNext()) {
        val current = iterator.next()
        val updated = transformer(current)
        iterator.set(transformer(current))
        if (current != updated) {
          out += updated
        }
      }
      out
    }
    every { mock.updateAllJobsToBePending() } answers {
      val iterator = jobs.listIterator()
      while (iterator.hasNext()) {
        val job = iterator.next()
        iterator.set(job.copy(isRunning = false))
      }
    }
    every { mock.updateJobAfterRetry(any(), any(), any(), any(), any()) } answers {
      val id = args[0] as String
      val currentTime = args[1] as Long
      val runAttempt = args[2] as Int
      val nextBackoffInterval = args[3] as Long
      val serializedData = args[4] as ByteArray?

      val iterator = jobs.listIterator()
      while (iterator.hasNext()) {
        val job = iterator.next()
        if (job.id == id) {
          iterator.set(
            job.copy(
              isRunning = false,
              runAttempt = runAttempt,
              lastRunAttemptTime = currentTime,
              nextBackoffInterval = nextBackoffInterval,
              serializedData = serializedData
            )
          )
        }
      }
    }
    every { mock.getMostEligibleJobInQueue(any()) } answers {
      jobs
        .asSequence()
        .filter { it.queueKey == firstArg() }
        .sortedBy { it.createTime }
        .sortedByDescending { it.queuePriority }
        .maxByOrNull { it.globalPriority }
    }

    return mock
  }

  private fun jobSpec(
    id: String,
    factoryKey: String,
    queueKey: String? = null,
    createTime: Long = 1,
    lastRunAttemptTime: Long = 1,
    nextBackoffInterval: Long = 0,
    runAttempt: Int = 1,
    maxAttempts: Int = 1,
    lifespan: Long = 1,
    serializedData: ByteArray? = null,
    serializedInputData: ByteArray? = null,
    isRunning: Boolean = false,
    isMemoryOnly: Boolean = false,
    globalPriority: Int = 0,
    queuePriority: Int = 0,
    initialDelay: Long = 0
  ): JobSpec {
    return JobSpec(
      id = id,
      factoryKey = factoryKey,
      queueKey = queueKey,
      createTime = createTime,
      lastRunAttemptTime = lastRunAttemptTime,
      nextBackoffInterval = nextBackoffInterval,
      runAttempt = runAttempt,
      maxAttempts = maxAttempts,
      lifespan = lifespan,
      serializedData = serializedData,
      serializedInputData = serializedInputData,
      isRunning = isRunning,
      isMemoryOnly = isMemoryOnly,
      globalPriority = globalPriority,
      queuePriority = queuePriority,
      initialDelay = initialDelay
    )
  }

  private object DataSet1 {
    val JOB_1 = JobSpec(
      id = "id1",
      factoryKey = "f1",
      queueKey = "q1",
      createTime = 1,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )
    val JOB_2 = JobSpec(
      id = "id2",
      factoryKey = "f2",
      queueKey = "q2",
      createTime = 1,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )
    val JOB_3 = JobSpec(
      id = "id3",
      factoryKey = "f3",
      queueKey = "q3",
      createTime = 1,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )

    val CONSTRAINT_1 = ConstraintSpec(jobSpecId = "id1", factoryKey = "f1", isMemoryOnly = false)
    val CONSTRAINT_2 = ConstraintSpec(jobSpecId = "id2", factoryKey = "f2", isMemoryOnly = false)

    val DEPENDENCY_2 = DependencySpec(jobId = "id2", dependsOnJobId = "id1", isMemoryOnly = false)
    val DEPENDENCY_3 = DependencySpec(jobId = "id3", dependsOnJobId = "id2", isMemoryOnly = false)

    val FULL_SPEC_1 = FullSpec(JOB_1, listOf(CONSTRAINT_1), emptyList())
    val FULL_SPEC_2 = FullSpec(JOB_2, listOf(CONSTRAINT_2), listOf(DEPENDENCY_2))
    val FULL_SPEC_3 = FullSpec(JOB_3, emptyList(), listOf(DEPENDENCY_3))
    val FULL_SPECS = listOf(FULL_SPEC_1, FULL_SPEC_2, FULL_SPEC_3)
    fun assertJobsMatch(jobs: List<JobSpec?>) {
      assertThat(jobs.size).isEqualTo(3)
      assertThat(jobs.contains(JOB_1)).isEqualTo(true)
      assertThat(jobs.contains(JOB_2)).isEqualTo(true)
      assertThat(jobs.contains(JOB_3)).isEqualTo(true)
    }

    fun assertConstraintsMatch(constraints: List<ConstraintSpec?>) {
      assertThat(constraints.size).isEqualTo(2)
      assertThat(constraints.contains(CONSTRAINT_1)).isEqualTo(true)
      assertThat(constraints.contains(CONSTRAINT_2)).isEqualTo(true)
    }

    fun assertDependenciesMatch(dependencies: List<DependencySpec?>) {
      assertThat(dependencies.size).isEqualTo(2)
      assertThat(dependencies.contains(DEPENDENCY_2)).isEqualTo(true)
      assertThat(dependencies.contains(DEPENDENCY_3)).isEqualTo(true)
    }
  }

  private object DataSetMemory {
    val JOB_1 = JobSpec(
      id = "id1",
      factoryKey = "f1",
      queueKey = "q1",
      createTime = 1,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = true,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )
    val CONSTRAINT_1 = ConstraintSpec(jobSpecId = "id1", factoryKey = "f1", isMemoryOnly = true)
    val FULL_SPEC_1 = FullSpec(JOB_1, listOf(CONSTRAINT_1), emptyList())
    val FULL_SPECS = listOf(FULL_SPEC_1)
  }

  private object DataSetCircularDependency {
    val JOB_1 = JobSpec(
      id = "id1",
      factoryKey = "f1",
      queueKey = "q1",
      createTime = 1,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )
    val JOB_2 = JobSpec(
      id = "id2",
      factoryKey = "f2",
      queueKey = "q1",
      createTime = 2,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )
    val JOB_3 = JobSpec(
      id = "id3",
      factoryKey = "f3",
      queueKey = "q3",
      createTime = 3,
      lastRunAttemptTime = 2,
      nextBackoffInterval = 0,
      runAttempt = 3,
      maxAttempts = 4,
      lifespan = 5,
      serializedData = null,
      serializedInputData = null,
      isRunning = false,
      isMemoryOnly = false,
      globalPriority = 0,
      queuePriority = 0,
      initialDelay = 0
    )

    val DEPENDENCY_1 = DependencySpec(jobId = "id1", dependsOnJobId = "id2", isMemoryOnly = false)
    val DEPENDENCY_3 = DependencySpec(jobId = "id3", dependsOnJobId = "id2", isMemoryOnly = false)

    val FULL_SPEC_1 = FullSpec(jobSpec = JOB_1, constraintSpecs = emptyList(), dependencySpecs = listOf(DEPENDENCY_1))
    val FULL_SPEC_2 = FullSpec(jobSpec = JOB_2, constraintSpecs = emptyList(), dependencySpecs = emptyList())
    val FULL_SPEC_3 = FullSpec(jobSpec = JOB_3, constraintSpecs = emptyList(), dependencySpecs = listOf(DEPENDENCY_3))
    val FULL_SPECS = listOf(FULL_SPEC_1, FULL_SPEC_2, FULL_SPEC_3)

    fun assertJobsMatch(jobs: List<JobSpec?>) {
      assertThat(jobs.size).isEqualTo(3)
      assertThat(jobs.contains(JOB_1)).isEqualTo(true)
      assertThat(jobs.contains(JOB_2)).isEqualTo(true)
      assertThat(jobs.contains(JOB_3)).isEqualTo(true)
    }

    fun assertConstraintsMatch(constraints: List<ConstraintSpec?>) {
      assertThat(constraints.size).isEqualTo(0)
    }

    fun assertDependenciesMatch(dependencies: List<DependencySpec?>) {
      assertThat(dependencies.size).isEqualTo(1)
      assertThat(dependencies.contains(DEPENDENCY_1)).isEqualTo(false)
      assertThat(dependencies.contains(DEPENDENCY_3)).isEqualTo(true)
    }
  }
}
