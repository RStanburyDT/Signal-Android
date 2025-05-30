/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.thoughtcrime.securesms.ryan.testutil.MockRandom
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class BackPendingParticipantsListenerTest {

  @Test
  fun testBackupJitterExactlyWithinJitterWindow() {
    val jitterWindowSeconds = Math.toIntExact(TimeUnit.MINUTES.toSeconds(10))
    val now = LocalDateTime.of(2024, 6, 7, 2, 55)
    val next = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, 3, 0, jitterWindowSeconds)
    assertEquals(8, next.dayOfMonth)
  }

  @Test
  fun testBackupJitterWithinJitterWindow() {
    val jitterWindowSeconds = Math.toIntExact(TimeUnit.MINUTES.toSeconds(10))
    val now = LocalDateTime.of(2024, 6, 7, 2, 58)
    val next = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, 3, 0, jitterWindowSeconds)
    assertEquals(8, next.dayOfMonth)
  }

  @Test
  fun testBackupJitterJustOutsideOfWindow() {
    val jitterWindowSeconds = Math.toIntExact(TimeUnit.MINUTES.toSeconds(10))
    val now = LocalDateTime.of(2024, 6, 7, 2, 54, 59)
    val next = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, 3, 0, jitterWindowSeconds)
    assertEquals(7, next.dayOfMonth)
  }

  @Test
  fun testBackupJitter() {
    val jitterWindowSeconds = Math.toIntExact(TimeUnit.MINUTES.toSeconds(10))
    val now = LocalDateTime.of(2024, 6, 7, 3, 15, 0)
    val next = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, 3, 0, jitterWindowSeconds)
    assertEquals(8, next.dayOfMonth)
  }

  @Test
  fun testBackupJitterWhenScheduledForMidnightButJitterMakesItRunJustBefore() {
    val mockRandom = MockRandom(listOf(1.minutes.inWholeSeconds.toInt()))
    val jitterWindowSeconds = 10.minutes.inWholeSeconds.toInt()
    val now: LocalDateTime = LocalDateTime.of(2024, 6, 27, 23, 57, 0)
    val next: LocalDateTime = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, 0, 0, jitterWindowSeconds, mockRandom)

    assertTrue(Duration.between(now, next).toSeconds() > (1.days.inWholeSeconds - jitterWindowSeconds))
  }
}
