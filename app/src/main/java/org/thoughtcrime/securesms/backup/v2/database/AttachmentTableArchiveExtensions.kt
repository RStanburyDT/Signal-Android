/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.backup.v2.database

import org.thoughtcrime.securesms.ryan.attachments.Attachment
import org.thoughtcrime.securesms.ryan.attachments.AttachmentId
import org.thoughtcrime.securesms.ryan.database.AttachmentTable

fun AttachmentTable.restoreWallpaperAttachment(attachment: Attachment): AttachmentId? {
  return insertAttachmentsForMessage(AttachmentTable.WALLPAPER_MESSAGE_ID, listOf(attachment), emptyList()).values.firstOrNull()
}
