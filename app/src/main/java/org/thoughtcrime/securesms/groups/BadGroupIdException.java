package org.thoughtcrime.securesms.ryan.groups;

import androidx.annotation.NonNull;

public final class BadGroupIdException extends Exception {

  BadGroupIdException() {
    super();
  }

  BadGroupIdException(@NonNull String message) {
    super(message);
  }

  BadGroupIdException(@NonNull Exception e) {
    super(e);
  }
}
