package org.thoughtcrime.securesms.ryan.service.webrtc;

import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.signal.ringrtc.CallException;
import org.signal.ringrtc.GroupCall;
import org.thoughtcrime.securesms.ryan.events.WebRtcViewModel;
import org.thoughtcrime.securesms.ryan.ringrtc.Camera;
import org.thoughtcrime.securesms.ryan.ringrtc.RemotePeer;
import org.thoughtcrime.securesms.ryan.service.webrtc.state.WebRtcServiceState;
import org.thoughtcrime.securesms.ryan.service.webrtc.state.WebRtcServiceStateBuilder;
import org.thoughtcrime.securesms.ryan.util.NetworkUtil;
import org.thoughtcrime.securesms.ryan.webrtc.locks.LockManager;

import static org.thoughtcrime.securesms.ryan.webrtc.CallNotificationBuilder.TYPE_ESTABLISHED;

/**
 * Process actions to go from lobby to a joined call.
 */
public class GroupJoiningActionProcessor extends GroupActionProcessor {

  private static final String TAG = Log.tag(GroupJoiningActionProcessor.class);

  public GroupJoiningActionProcessor(@NonNull MultiPeerActionProcessorFactory actionProcessorFactory, @NonNull WebRtcInteractor webRtcInteractor) {
    this(actionProcessorFactory, webRtcInteractor, TAG);
  }

  protected GroupJoiningActionProcessor(@NonNull MultiPeerActionProcessorFactory actionProcessorFactory, @NonNull WebRtcInteractor webRtcInteractor, @NonNull String tag) {
    super(actionProcessorFactory, webRtcInteractor, tag);
  }

  @Override
  protected @NonNull WebRtcServiceState handleIsInCallQuery(@NonNull WebRtcServiceState currentState, @Nullable ResultReceiver resultReceiver) {
    if (resultReceiver != null) {
      resultReceiver.send(1, ActiveCallData.fromCallState(currentState).toBundle());
    }
    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleGroupLocalDeviceStateChanged(@NonNull WebRtcServiceState currentState) {
    Log.i(tag, "handleGroupLocalDeviceStateChanged():");

    currentState = super.handleGroupLocalDeviceStateChanged(currentState);

    GroupCall                  groupCall = currentState.getCallInfoState().requireGroupCall();
    GroupCall.LocalDeviceState device    = groupCall.getLocalDeviceState();

    Log.i(tag, "local device changed: " + device.getConnectionState() + " " + device.getJoinState());

    WebRtcServiceStateBuilder builder = currentState.builder();

    switch (device.getConnectionState()) {
      case NOT_CONNECTED:
      case RECONNECTING:
        builder.changeCallInfoState()
               .groupCallState(WebRtcUtil.groupCallStateForConnection(device.getConnectionState()))
               .commit();
        break;
      case CONNECTING:
      case CONNECTED:
        if (device.getJoinState() == GroupCall.JoinState.JOINED) {

          webRtcInteractor.setCallInProgressNotification(TYPE_ESTABLISHED, currentState.getCallInfoState().getCallRecipient(), true);
          webRtcInteractor.startAudioCommunication();

          if (currentState.getLocalDeviceState().getCameraState().isEnabled()) {
            webRtcInteractor.updatePhoneState(LockManager.PhoneState.IN_VIDEO);
          } else {
            webRtcInteractor.updatePhoneState(WebRtcUtil.getInCallPhoneState(context));
          }

          try {
            groupCall.setOutgoingVideoMuted(!currentState.getLocalDeviceState().getCameraState().isEnabled());
            groupCall.setOutgoingAudioMuted(!currentState.getLocalDeviceState().isMicrophoneEnabled());
            groupCall.setDataMode(NetworkUtil.getCallingDataMode(context, device.getNetworkRoute().getLocalAdapterType()));
          } catch (CallException e) {
            Log.e(tag, e);
            throw new RuntimeException(e);
          }

          if (currentState.getCallSetupState(RemotePeer.GROUP_CALL_ID).shouldRingGroup()) {
            try {
              groupCall.ringAll();
            } catch (CallException e) {
              return groupCallFailure(currentState, "Unable to ring group", e);
            }
          }

          currentState = builder.changeCallInfoState()
                                .callState(WebRtcViewModel.State.CALL_CONNECTED)
                                .groupCallState(WebRtcViewModel.GroupCallState.CONNECTED_AND_JOINED)
                                .callConnectedTime(System.currentTimeMillis())
                                .commit()
                                .changeLocalDeviceState()
                                .commit()
                                .actionProcessor(actionProcessorFactory.createConnectedActionProcessor(webRtcInteractor))
                                .build();

          builder = currentState.getActionProcessor().handleGroupJoinedMembershipChanged(currentState).builder();
        } else if (device.getJoinState() == GroupCall.JoinState.JOINING) {
          builder.changeCallInfoState()
                 .groupCallState(WebRtcViewModel.GroupCallState.CONNECTED_AND_JOINING)
                 .commit();
        } else if (device.getJoinState() == GroupCall.JoinState.PENDING) {
          builder.changeCallInfoState()
                 .groupCallState(WebRtcViewModel.GroupCallState.CONNECTED_AND_PENDING)
                 .commit();
        }else {
          builder.changeCallInfoState()
                 .groupCallState(WebRtcUtil.groupCallStateForConnection(device.getConnectionState()))
                 .commit();
        }
        break;
    }

    return builder.build();
  }

  protected @NonNull WebRtcServiceState handleLocalHangup(@NonNull WebRtcServiceState currentState) {
    Log.i(tag, "handleLocalHangup():");

    GroupCall groupCall = currentState.getCallInfoState().requireGroupCall();
    try {
      groupCall.disconnect();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to disconnect from group call", e);
    }

    currentState = currentState.builder()
                               .changeCallInfoState()
                               .callState(WebRtcViewModel.State.CALL_DISCONNECTED)
                               .groupCallState(WebRtcViewModel.GroupCallState.DISCONNECTED)
                               .build();

    webRtcInteractor.postStateUpdate(currentState);

    return terminateGroupCall(currentState);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetEnableVideo(@NonNull WebRtcServiceState currentState, boolean enable) {
    GroupCall groupCall = currentState.getCallInfoState().requireGroupCall();
    Camera    camera    = currentState.getVideoState().requireCamera();

    try {
      groupCall.setOutgoingVideoMuted(!enable);
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to set video muted", e);
    }
    camera.setEnabled(enable);

    currentState = currentState.builder()
                               .changeLocalDeviceState()
                               .cameraState(camera.getCameraState())
                               .build();

    WebRtcUtil.enableSpeakerPhoneIfNeeded(webRtcInteractor, currentState);

    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetMuteAudio(@NonNull WebRtcServiceState currentState, boolean muted) {
    try {
      currentState.getCallInfoState().requireGroupCall().setOutgoingAudioMuted(muted);
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to set audio muted", e);
    }

    return currentState.builder()
                       .changeLocalDeviceState()
                       .isMicrophoneEnabled(!muted)
                       .build();
  }
}
