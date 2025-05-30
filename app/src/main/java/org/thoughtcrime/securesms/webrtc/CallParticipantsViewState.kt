package org.thoughtcrime.securesms.ryan.webrtc

import org.thoughtcrime.securesms.ryan.components.webrtc.CallParticipantsState
import org.thoughtcrime.securesms.ryan.service.webrtc.state.WebRtcEphemeralState

class CallParticipantsViewState(
  callParticipantsState: CallParticipantsState,
  ephemeralState: WebRtcEphemeralState,
  val isPortrait: Boolean,
  val isLandscapeEnabled: Boolean
) {

  val callParticipantsState = CallParticipantsState.update(callParticipantsState, ephemeralState)
}
