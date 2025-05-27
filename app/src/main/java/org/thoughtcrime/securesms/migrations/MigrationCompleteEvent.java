package org.thoughtcrime.securesms.ryan.migrations;

public class MigrationCompleteEvent {

  private final int version;

  public MigrationCompleteEvent(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }
}
