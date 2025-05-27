package org.thoughtcrime.securesms.ryan.groups;

import androidx.annotation.NonNull;

public abstract class GroupChangeException extends Exception {

  GroupChangeException() {
  }

  GroupChangeException(@NonNull Throwable throwable) {
    super(throwable);
  }

  GroupChangeException(@NonNull String message) {
    super(message);
  }
}
