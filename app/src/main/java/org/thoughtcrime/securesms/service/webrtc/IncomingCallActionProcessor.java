package org.thoughtcrime.securesms.ryan.service.webrtc;

import android.net.Uri;
import android.os.ResultReceiver;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.signal.ringrtc.CallException;
import org.signal.ringrtc.CallId;
import org.signal.ringrtc.CallManager;
import org.thoughtcrime.securesms.ryan.database.CallTable;
import org.thoughtcrime.securesms.ryan.database.RecipientTable;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.events.CallParticipant;
import org.thoughtcrime.securesms.ryan.events.WebRtcViewModel;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.notifications.DoNotDisturbUtil;
import org.thoughtcrime.securesms.ryan.notifications.NotificationChannels;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;
import org.thoughtcrime.securesms.ryan.ringrtc.CallState;
import org.thoughtcrime.securesms.ryan.ringrtc.RemotePeer;
import org.thoughtcrime.securesms.ryan.service.webrtc.state.CallSetupState;
import org.thoughtcrime.securesms.ryan.service.webrtc.state.VideoState;
import org.thoughtcrime.securesms.ryan.service.webrtc.state.WebRtcServiceState;
import org.thoughtcrime.securesms.ryan.util.AppForegroundObserver;
import org.thoughtcrime.securesms.ryan.util.NetworkUtil;
import org.thoughtcrime.securesms.ryan.util.Util;
import org.thoughtcrime.securesms.ryan.webrtc.locks.LockManager;
import org.webrtc.PeerConnection;

import java.util.List;
import java.util.Objects;

import static org.thoughtcrime.securesms.ryan.webrtc.CallNotificationBuilder.TYPE_INCOMING_RINGING;

/**
 * Responsible for setting up and managing the start of an incoming 1:1 call. Transitioned
 * to from idle or pre-join and can either move to a connected state (user picks up) or
 * a disconnected state (remote hangup, local hangup, etc.).
 */
public class IncomingCallActionProcessor extends DeviceAwareActionProcessor {

  private static final String TAG = Log.tag(IncomingCallActionProcessor.class);

  private final ActiveCallActionProcessorDelegate activeCallDelegate;
  private final CallSetupActionProcessorDelegate  callSetupDelegate;

  public IncomingCallActionProcessor(@NonNull WebRtcInteractor webRtcInteractor) {
    super(webRtcInteractor, TAG);
    activeCallDelegate = new ActiveCallActionProcessorDelegate(webRtcInteractor, TAG);
    callSetupDelegate  = new CallSetupActionProcessorDelegate(webRtcInteractor, TAG);
  }

  @Override
  protected @NonNull WebRtcServiceState handleIsInCallQuery(@NonNull WebRtcServiceState currentState, @Nullable ResultReceiver resultReceiver) {
    return activeCallDelegate.handleIsInCallQuery(currentState, resultReceiver);
  }

  @Override
  public @NonNull WebRtcServiceState handleTurnServerUpdate(@NonNull WebRtcServiceState currentState,
                                                            @NonNull List<PeerConnection.IceServer> iceServers,
                                                            boolean isAlwaysTurn)
  {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    Log.i(TAG, "handleTurnServerUpdate(): call_id: " + activePeer.getCallId());

    currentState = currentState.builder()
                               .changeCallSetupState(activePeer.getCallId())
                               .iceServers(iceServers)
                               .alwaysTurn(isAlwaysTurn)
                               .build();

    return proceed(currentState);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetTelecomApproved(@NonNull WebRtcServiceState currentState, long callId, RecipientId recipientId) {
    return proceed(super.handleSetTelecomApproved(currentState, callId, recipientId));
  }

  private @NonNull WebRtcServiceState proceed(@NonNull WebRtcServiceState currentState) {
    RemotePeer     activePeer     = currentState.getCallInfoState().requireActivePeer();
    CallSetupState callSetupState = currentState.getCallSetupState(activePeer.getCallId());

    if (callSetupState.getIceServers().isEmpty() || (callSetupState.shouldWaitForTelecomApproval() && !callSetupState.isTelecomApproved())) {
      Log.i(TAG, "Unable to proceed without ice server and telecom approval" +
                 " iceServers: " + Util.hasItems(callSetupState.getIceServers()) +
                 " waitForTelecom: " + callSetupState.shouldWaitForTelecomApproval() +
                 " telecomApproved: " + callSetupState.isTelecomApproved());
      return currentState;
    }

    boolean         hideIp          = !activePeer.getRecipient().isProfileSharing() || callSetupState.isAlwaysTurnServers();
    VideoState      videoState      = currentState.getVideoState();
    CallParticipant callParticipant = Objects.requireNonNull(currentState.getCallInfoState().getRemoteCallParticipant(activePeer.getRecipient()));

    try {
      webRtcInteractor.getCallManager().proceed(activePeer.getCallId(),
                                                context,
                                                videoState.getLockableEglBase().require(),
                                                RingRtcDynamicConfiguration.getAudioConfig(),
                                                videoState.requireLocalSink(),
                                                callParticipant.getVideoSink(),
                                                videoState.requireCamera(),
                                                callSetupState.getIceServers(),
                                                hideIp,
                                                NetworkUtil.getCallingDataMode(context),
                                                AUDIO_LEVELS_INTERVAL,
                                                false);
    } catch (CallException e) {
      return callFailure(currentState, "Unable to proceed with call: ", e);
    }

    webRtcInteractor.updatePhoneState(LockManager.PhoneState.PROCESSING);
    webRtcInteractor.postStateUpdate(currentState);

    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleDropCall(@NonNull WebRtcServiceState currentState, long callId) {
    return callSetupDelegate.handleDropCall(currentState, callId);
  }

  @Override
  protected @NonNull WebRtcServiceState handleAcceptCall(@NonNull WebRtcServiceState currentState, boolean answerWithVideo) {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    Log.i(TAG, "handleAcceptCall(): call_id: " + activePeer.getCallId());

    currentState = currentState.builder()
                               .changeCallSetupState(activePeer.getCallId())
                               .acceptWithVideo(answerWithVideo)
                               .build();

    try {
      webRtcInteractor.getCallManager().acceptCall(activePeer.getCallId());
    } catch (CallException e) {
      return callFailure(currentState, "accept() failed: ", e);
    }

    return currentState;
  }

  protected @NonNull WebRtcServiceState handleDenyCall(@NonNull WebRtcServiceState currentState) {
    RemotePeer activePeer = currentState.getCallInfoState().requireActivePeer();

    if (activePeer.getState() != CallState.LOCAL_RINGING) {
      Log.w(TAG, "Can only deny from ringing!");
      return currentState;
    }

    Log.i(TAG, "handleDenyCall():");

    webRtcInteractor.sendNotAcceptedCallEventSyncMessage(activePeer,
                                                         false,
                                                         currentState.getCallSetupState(activePeer).isRemoteVideoOffer());

    try {
      webRtcInteractor.rejectIncomingCall(activePeer.getId());
      webRtcInteractor.getCallManager().hangup();
      return terminate(currentState, activePeer);
    } catch (CallException e) {
      return callFailure(currentState, "hangup() failed: ", e);
    }
  }

  protected @NonNull WebRtcServiceState handleLocalRinging(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    Log.i(TAG, "handleLocalRinging(): call_id: " + remotePeer.getCallId());

    RemotePeer activePeer                = currentState.getCallInfoState().requireActivePeer();
    Recipient  recipient                 = remotePeer.getRecipient();
    boolean    shouldDisturbUserWithCall = DoNotDisturbUtil.shouldDisturbUserWithCall(context.getApplicationContext(), recipient);

    activePeer.localRinging();

    SignalDatabase.calls().insertOneToOneCall(remotePeer.getCallId().longValue(),
                                              System.currentTimeMillis(),
                                              remotePeer.getId(),
                                      currentState.getCallSetupState(activePeer).isRemoteVideoOffer() ? CallTable.Type.VIDEO_CALL : CallTable.Type.AUDIO_CALL,
                                              CallTable.Direction.INCOMING,
                                              CallTable.Event.ONGOING);


    if (shouldDisturbUserWithCall) {
      webRtcInteractor.updatePhoneState(LockManager.PhoneState.INTERACTIVE);
      boolean started = webRtcInteractor.startWebRtcCallActivityIfPossible();
      if (!started) {
        Log.i(TAG, "Unable to start call activity due to OS version or not being in the foreground");
        AppForegroundObserver.addListener(webRtcInteractor.getForegroundListener());
      }
    }

    boolean isCallNotificationsEnabled = SignalStore.settings().isCallNotificationsEnabled() && NotificationChannels.getInstance().areNotificationsEnabled();
    if (shouldDisturbUserWithCall && isCallNotificationsEnabled) {
      Uri                         ringtone     = recipient.resolve().getCallRingtone();
      RecipientTable.VibrateState vibrateState = recipient.resolve().getCallVibrate();

      if (ringtone == null) {
        ringtone = SignalStore.settings().getCallRingtone();
      }

      if (TextUtils.isEmpty(ringtone.toString())) {
        Log.i(TAG, "Ringtone is likely set to silent");
        ringtone = null;
      }

      webRtcInteractor.startIncomingRinger(ringtone, vibrateState == RecipientTable.VibrateState.ENABLED || (vibrateState == RecipientTable.VibrateState.DEFAULT && SignalStore.settings().isCallVibrateEnabled()));
    }

    boolean isRemoteVideoOffer = currentState.getCallSetupState(activePeer).isRemoteVideoOffer();

    webRtcInteractor.setCallInProgressNotification(TYPE_INCOMING_RINGING, activePeer, isRemoteVideoOffer);
    webRtcInteractor.registerPowerButtonReceiver();

    return currentState.builder()
                       .changeCallInfoState()
                       .callState(WebRtcViewModel.State.CALL_INCOMING)
                       .build();
  }

  protected @NonNull WebRtcServiceState handleScreenOffChange(@NonNull WebRtcServiceState currentState) {
    Log.i(TAG, "Silencing incoming ringer...");

    webRtcInteractor.silenceIncomingRinger();
    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleRemoteAudioEnable(@NonNull WebRtcServiceState currentState, boolean enable) {
    return activeCallDelegate.handleRemoteAudioEnable(currentState, enable);
  }

  @Override
  protected @NonNull WebRtcServiceState handleRemoteVideoEnable(@NonNull WebRtcServiceState currentState, boolean enable) {
    return activeCallDelegate.handleRemoteVideoEnable(currentState, enable);
  }

  @Override
  protected @NonNull WebRtcServiceState handleScreenSharingEnable(@NonNull WebRtcServiceState currentState, boolean enable) {
    return activeCallDelegate.handleScreenSharingEnable(currentState, enable);
  }

  @Override
  protected @NonNull WebRtcServiceState handleReceivedOfferWhileActive(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleReceivedOfferWhileActive(currentState, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleEndedRemote(@NonNull WebRtcServiceState currentState, @NonNull CallManager.CallEvent endedRemoteEvent, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleEndedRemote(currentState, endedRemoteEvent, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleEnded(@NonNull WebRtcServiceState currentState, @NonNull CallManager.CallEvent endedEvent, @NonNull RemotePeer remotePeer) {
    return activeCallDelegate.handleEnded(currentState, endedEvent, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetupFailure(@NonNull WebRtcServiceState currentState, @NonNull CallId callId) {
    return activeCallDelegate.handleSetupFailure(currentState, callId);
  }

  @Override
  public @NonNull WebRtcServiceState handleCallConnected(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    return callSetupDelegate.handleCallConnected(currentState, remotePeer);
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetEnableVideo(@NonNull WebRtcServiceState currentState, boolean enable) {
    return callSetupDelegate.handleSetEnableVideo(currentState, enable);
  }
}
