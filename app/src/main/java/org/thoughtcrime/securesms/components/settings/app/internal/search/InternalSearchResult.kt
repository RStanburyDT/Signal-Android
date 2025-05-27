/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.settings.app.internal.search

import org.thoughtcrime.securesms.ryan.groups.GroupId
import org.thoughtcrime.securesms.ryan.recipients.RecipientId

data class InternalSearchResult(
  val name: String,
  val id: RecipientId,
  val aci: String? = null,
  val pni: String? = null,
  val groupId: GroupId? = null
)
