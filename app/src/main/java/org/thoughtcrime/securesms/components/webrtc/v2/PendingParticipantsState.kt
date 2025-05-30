/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.webrtc.v2

import org.thoughtcrime.securesms.ryan.service.webrtc.PendingParticipantCollection

/**
 * Represents the current state of the pending participants card.
 */
data class PendingParticipantsState(
  val pendingParticipantCollection: PendingParticipantCollection,
  val isInPipMode: Boolean
)
