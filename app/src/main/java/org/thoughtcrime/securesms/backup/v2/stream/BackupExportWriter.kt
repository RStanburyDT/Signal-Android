/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.backup.v2.stream

import org.thoughtcrime.securesms.ryan.backup.v2.proto.BackupInfo
import org.thoughtcrime.securesms.ryan.backup.v2.proto.Frame

interface BackupExportWriter : AutoCloseable {
  fun write(header: BackupInfo)
  fun write(frame: Frame)
}
