/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.webrtc.v2

<<<<<<< HEAD
import org.thoughtcrime.securesms.ryan.database.model.IdentityRecord
=======
import android.content.Context
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.model.IdentityRecord
import org.thoughtcrime.securesms.recipients.Recipient
>>>>>>> 23669c3c372284d42db486a218d9f29bef247abf

/**
 * Replacement sealed class for WebRtcCallViewModel.Event
 */
sealed interface CallEvent {
  data object ShowVideoTooltip : CallEvent
  data object DismissVideoTooltip : CallEvent
  data object ShowWifiToCellularPopup : CallEvent
  data object ShowSwitchCameraTooltip : CallEvent
  data object DismissSwitchCameraTooltip : CallEvent
  data class StartCall(val isVideoCall: Boolean) : CallEvent
  data class ShowGroupCallSafetyNumberChange(val identityRecords: List<IdentityRecord>) : CallEvent
  data object SwitchToSpeaker : CallEvent
  data object ShowSwipeToSpeakerHint : CallEvent
  data class ShowRemoteMuteToast(private val muted: Recipient, private val mutedBy: Recipient) : CallEvent {
    fun getDescription(context: Context): String {
      return if (muted.isSelf && mutedBy.isSelf) {
        context.getString(R.string.WebRtcCallView__you_muted_yourself)
      } else if (muted.isSelf) {
        context.getString(R.string.WebRtcCallView__s_remotely_muted_you, mutedBy.getDisplayName(context))
      } else if (mutedBy.isSelf) {
        context.getString(R.string.WebRtcCallView__you_remotely_muted_s, muted.getDisplayName(context))
      } else {
        context.getString(R.string.WebRtcCallView__s_remotely_muted_s, mutedBy.getDisplayName(context), muted.getDisplayName(context))
      }
    }
  }
}
