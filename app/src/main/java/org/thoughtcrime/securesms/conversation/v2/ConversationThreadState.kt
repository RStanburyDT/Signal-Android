/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.conversation.v2

import org.signal.paging.ObservablePagedData
import org.thoughtcrime.securesms.ryan.conversation.ConversationData
import org.thoughtcrime.securesms.ryan.conversation.v2.data.ConversationElementKey
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

/**
 * Represents the content that will be displayed in the conversation
 * thread (recycler).
 */
class ConversationThreadState(
  val items: ObservablePagedData<ConversationElementKey, MappingModel<*>>,
  val meta: ConversationData
)
