/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.jobmanager.migrations

import org.thoughtcrime.securesms.ryan.jobmanager.JobMigration

/**
 * Used as a replacement for another JobMigration that is no longer necessary.
 */
class DeprecatedJobMigration(version: Int) : JobMigration(version) {
  override fun migrate(jobData: JobData): JobData = jobData
}
