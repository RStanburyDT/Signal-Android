/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.restore.restorelocalbackup

import android.net.Uri
import org.thoughtcrime.securesms.ryan.restore.RestoreRepository
import org.thoughtcrime.securesms.ryan.util.BackupUtil
import org.thoughtcrime.securesms.ryan.util.BackupUtil.BackupInfo

/**
 * State holder for a backup restore.
 */
data class RestoreLocalBackupState(
  val uri: Uri,
  val backupInfo: BackupInfo? = null,
  val backupFileStateError: BackupUtil.BackupFileState? = null,
  val backupPassphrase: String = "",
  val restoreInProgress: Boolean = false,
  val backupVerifyingInProgress: Boolean = false,
  val backupProgressCount: Long = -1,
  val backupEstimatedTotalCount: Long = -1,
  val backupImportResult: RestoreRepository.BackupImportResult? = null,
  val abort: Boolean = false
)
