package org.thoughtcrime.securesms.ryan.groups;

public final class GroupDoesNotExistException extends GroupChangeException {

  public GroupDoesNotExistException(Throwable throwable) {
    super(throwable);
  }
}
