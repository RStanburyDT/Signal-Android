package org.thoughtcrime.securesms.ryan.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.jobmanager.Job;
import org.thoughtcrime.securesms.ryan.jobmanager.impl.NetworkConstraint;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.net.SignalNetwork;
import org.thoughtcrime.securesms.ryan.util.ExceptionHelper;
import org.thoughtcrime.securesms.ryan.util.RemoteConfig;
import org.whispersystems.signalservice.api.NetworkResultUtil;
import org.whispersystems.signalservice.api.remoteconfig.RemoteConfigResult;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.util.concurrent.TimeUnit;

public class RemoteConfigRefreshJob extends BaseJob {

  private static final String TAG = Log.tag(RemoteConfigRefreshJob.class);

  public static final String KEY = "RemoteConfigRefreshJob";

  public RemoteConfigRefreshJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("RemoteConfigRefreshJob")
                           .setMaxInstancesForFactory(1)
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .build());
  }

  private RemoteConfigRefreshJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @Nullable byte[] serialize() {
    return null;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (!SignalStore.account().isRegistered()) {
      Log.w(TAG, "Not registered. Skipping.");
      return;
    }

    RemoteConfigResult result = NetworkResultUtil.toBasicLegacy(SignalNetwork.remoteConfig().getRemoteConfig());
    RemoteConfig.update(result.getConfig());
    SignalStore.misc().setLastKnownServerTime(result.getServerEpochTimeMilliseconds(), System.currentTimeMillis());
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return ExceptionHelper.isRetryableIOException(e);
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<RemoteConfigRefreshJob> {
    @Override
    public @NonNull RemoteConfigRefreshJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new RemoteConfigRefreshJob(parameters);
    }
  }
}
