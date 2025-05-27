package org.thoughtcrime.securesms.ryan.jobmanager.impl;

import android.app.job.JobInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.jobmanager.Constraint;

/**
 * A constraint that is met once we have pulled down and decrypted all messages from the websocket
 * during initial load. See {@link org.thoughtcrime.securesms.ryan.messages.IncomingMessageObserver}.
 */
public final class DecryptionsDrainedConstraint implements Constraint {

  public static final String KEY = "WebsocketDrainedConstraint";

  private DecryptionsDrainedConstraint() {
  }

  @Override
  public boolean isMet() {
    return AppDependencies.getIncomingMessageObserver().getDecryptionDrained();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @RequiresApi(26)
  @Override
  public void applyToJobInfo(@NonNull JobInfo.Builder jobInfoBuilder) {
  }

  public static final class Factory implements Constraint.Factory<DecryptionsDrainedConstraint> {

    @Override
    public DecryptionsDrainedConstraint create() {
      return new DecryptionsDrainedConstraint();
    }
  }
}
