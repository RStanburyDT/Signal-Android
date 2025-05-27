/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.webrtc.v2

import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.TooltipPopup
import org.thoughtcrime.securesms.ryan.components.webrtc.CallOverflowPopupWindow
import org.thoughtcrime.securesms.ryan.components.webrtc.CallParticipantListUpdate
import org.thoughtcrime.securesms.ryan.components.webrtc.CallParticipantsListUpdatePopupWindow
import org.thoughtcrime.securesms.ryan.components.webrtc.CallParticipantsState
import org.thoughtcrime.securesms.ryan.components.webrtc.CallStateUpdatePopupWindow
import org.thoughtcrime.securesms.ryan.components.webrtc.WebRtcCallView
import org.thoughtcrime.securesms.ryan.components.webrtc.WebRtcControls
import org.thoughtcrime.securesms.ryan.components.webrtc.WifiToCellularPopupWindow
import org.thoughtcrime.securesms.ryan.components.webrtc.controls.ControlsAndInfoController
import org.thoughtcrime.securesms.ryan.components.webrtc.controls.ControlsAndInfoViewModel
import org.thoughtcrime.securesms.ryan.events.WebRtcViewModel
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.util.visible
import org.thoughtcrime.securesms.ryan.webrtc.CallParticipantsViewState

/**
 * Wraps WebRtcCallView and supporting code into a mediator subclass
 */
class ViewCallScreenMediator(
  private val activity: WebRtcCallActivity,
  private val viewModel: WebRtcCallViewModel
) : CallScreenMediator {
  private val callScreen: WebRtcCallView
  private val participantUpdateWindow: CallParticipantsListUpdatePopupWindow
  private val callStateUpdatePopupWindow: CallStateUpdatePopupWindow
  private val callOverflowPopupWindow: CallOverflowPopupWindow
  private val wifiToCellularPopupWindow: WifiToCellularPopupWindow
  private val controlsAndInfo: ControlsAndInfoController
  private val controlsAndInfoViewModel: ControlsAndInfoViewModel
  private val lifecycleDisposable = LifecycleDisposable()

  init {
    activity.setContentView(R.layout.webrtc_call_activity)
    callScreen = activity.findViewById(R.id.callScreen)

    participantUpdateWindow = CallParticipantsListUpdatePopupWindow(callScreen)
    callStateUpdatePopupWindow = CallStateUpdatePopupWindow(callScreen)
    wifiToCellularPopupWindow = WifiToCellularPopupWindow(callScreen)
    callOverflowPopupWindow = CallOverflowPopupWindow(activity, callScreen) {
      val state: CallParticipantsState = viewModel.callParticipantsStateSnapshot
      state.localParticipant.isHandRaised
    }

    activity.lifecycle.addObserver(participantUpdateWindow)

    controlsAndInfoViewModel = ViewModelProvider(activity)[ControlsAndInfoViewModel::class]
    controlsAndInfo = ControlsAndInfoController(activity, callScreen, callOverflowPopupWindow, viewModel, controlsAndInfoViewModel)

    lifecycleDisposable.bindTo(activity.lifecycle)
    lifecycleDisposable.add(controlsAndInfo)
  }

  override fun setWebRtcCallState(callState: WebRtcViewModel.State) = Unit

  override fun setControlsAndInfoVisibilityListener(listener: CallControlsVisibilityListener) {
    controlsAndInfo.addVisibilityListener(listener)
  }

  override fun onStateRestored() {
    controlsAndInfo.onStateRestored()
  }

  override fun toggleOverflowPopup() {
    controlsAndInfo.toggleOverflowPopup()
  }

  override fun restartHideControlsTimer() {
    controlsAndInfo.restartHideControlsTimer()
  }

  override fun showCallInfo() {
    controlsAndInfo.showCallInfo()
  }

  override fun toggleControls() {
    controlsAndInfo.toggleControls()
  }

  override fun setControlsListener(controlsListener: CallScreenControlsListener) {
    callScreen.setControlsListener(controlsListener)
  }

  override fun setMicEnabled(enabled: Boolean) {
    callScreen.setMicEnabled(enabled)
  }

  override fun setRecipient(recipient: Recipient) {
    controlsAndInfoViewModel.setRecipient(recipient)
    callScreen.setRecipient(recipient)
  }

  override fun setStatus(status: String) {
    callScreen.setStatus(status)
  }

  override fun setWebRtcControls(webRtcControls: WebRtcControls) {
    callScreen.setWebRtcControls(webRtcControls)
    controlsAndInfo.updateControls(webRtcControls)
  }

  override fun updateCallParticipants(callParticipantsViewState: CallParticipantsViewState) {
    callScreen.updateCallParticipants(callParticipantsViewState)
  }

  override fun maybeDismissAudioPicker() {
    callScreen.maybeDismissAudioPicker()
  }

  override fun setPendingParticipantsViewListener(pendingParticipantsViewListener: PendingParticipantsListener) {
    callScreen.setPendingParticipantsViewListener(pendingParticipantsViewListener)
  }

  override fun updatePendingParticipantsList(pendingParticipantsList: PendingParticipantsState) {
    callScreen.updatePendingParticipantsList(pendingParticipantsList)
  }

  override fun setRingGroup(ringGroup: Boolean) {
    callScreen.setRingGroup(ringGroup)
  }

  override fun switchToSpeakerView() {
    callScreen.switchToSpeakerView()
  }

  override fun enableRingGroup(canRing: Boolean) {
    callScreen.enableRingGroup(canRing)
  }

  override fun showSpeakerViewHint() {
    callScreen.showSpeakerViewHint()
  }

  override fun hideSpeakerViewHint() {
    callScreen.hideSpeakerViewHint()
  }

  override fun showVideoTooltip(): Dismissible {
    val tooltip = TooltipPopup.forTarget(callScreen.videoTooltipTarget)
      .setBackgroundTint(ContextCompat.getColor(activity, R.color.core_ultramarine))
      .setTextColor(ContextCompat.getColor(activity, R.color.core_white))
      .setText(R.string.WebRtcCallActivity__tap_here_to_turn_on_your_video)
      .setOnDismissListener { viewModel.onDismissedVideoTooltip() }
      .show(TooltipPopup.POSITION_ABOVE)

    return Dismissible {
      tooltip.dismiss()
    }
  }

  override fun showCameraTooltip(): Dismissible {
    val tooltip = TooltipPopup.forTarget(callScreen.switchCameraTooltipTarget)
      .setBackgroundTint(ContextCompat.getColor(activity, R.color.core_ultramarine))
      .setTextColor(ContextCompat.getColor(activity, R.color.core_white))
      .setText(R.string.WebRtcCallActivity__flip_camera_tooltip)
      .setOnDismissListener {
        viewModel.onDismissedSwitchCameraTooltip()
      }
      .show(TooltipPopup.POSITION_ABOVE)

    return Dismissible { tooltip.dismiss() }
  }

  override fun onCallStateUpdate(callControlsChange: CallControlsChange) {
    callStateUpdatePopupWindow.onCallStateUpdate(callControlsChange)
  }

  override fun dismissCallOverflowPopup() {
    callOverflowPopupWindow.dismiss()
  }

  override fun onParticipantListUpdate(callParticipantListUpdate: CallParticipantListUpdate) {
    participantUpdateWindow.addCallParticipantListUpdate(callParticipantListUpdate)
  }

  override fun enableParticipantUpdatePopup(enabled: Boolean) {
    participantUpdateWindow.setEnabled(enabled)
  }

  override fun enableCallStateUpdatePopup(enabled: Boolean) {
    callStateUpdatePopupWindow.setEnabled(enabled)
  }

  override fun showWifiToCellularPopupWindow() {
    wifiToCellularPopupWindow.show()
  }

  override fun hideMissingPermissionsNotice() {
    callScreen.findViewById<View>(R.id.missing_permissions_container).visible = false
  }
}
