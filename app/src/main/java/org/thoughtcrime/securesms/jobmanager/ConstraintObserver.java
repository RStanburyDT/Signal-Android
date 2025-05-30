package org.thoughtcrime.securesms.ryan.jobmanager;

import androidx.annotation.NonNull;

public interface ConstraintObserver {

  void register(@NonNull Notifier notifier);

  interface Notifier {
    void onConstraintMet(@NonNull String reason);
  }
}
