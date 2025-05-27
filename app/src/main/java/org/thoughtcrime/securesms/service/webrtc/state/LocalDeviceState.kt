package org.thoughtcrime.securesms.ryan.service.webrtc.state

import org.thoughtcrime.securesms.ryan.components.sensors.Orientation
import org.thoughtcrime.securesms.ryan.events.CallParticipant
import org.thoughtcrime.securesms.ryan.ringrtc.CameraState
import org.thoughtcrime.securesms.ryan.webrtc.audio.SignalAudioManager
import org.webrtc.PeerConnection

/**
 * Local device specific state.
 */
data class LocalDeviceState(
  var cameraState: CameraState = CameraState.UNKNOWN,
  var isMicrophoneEnabled: Boolean = true,
  var orientation: Orientation = Orientation.PORTRAIT_BOTTOM_EDGE,
  var isLandscapeEnabled: Boolean = false,
  var deviceOrientation: Orientation = Orientation.PORTRAIT_BOTTOM_EDGE,
  var activeDevice: SignalAudioManager.AudioDevice = SignalAudioManager.AudioDevice.NONE,
  var availableDevices: Set<SignalAudioManager.AudioDevice> = emptySet(),
  var bluetoothPermissionDenied: Boolean = false,
  var networkConnectionType: PeerConnection.AdapterType = PeerConnection.AdapterType.UNKNOWN,
  var handRaisedTimestamp: Long = CallParticipant.HAND_LOWERED,
  var remoteMutedBy: CallParticipant? = null
) {

  fun duplicate(): LocalDeviceState {
    return copy()
  }
}
