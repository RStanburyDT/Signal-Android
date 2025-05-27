package org.thoughtcrime.securesms.ryan.groups;

public final class GroupInsufficientRightsException extends GroupChangeException {

  GroupInsufficientRightsException(Throwable throwable) {
    super(throwable);
  }
}
