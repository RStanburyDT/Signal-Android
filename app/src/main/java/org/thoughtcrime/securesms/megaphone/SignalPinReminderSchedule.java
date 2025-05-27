package org.thoughtcrime.securesms.ryan.megaphone;

import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;

final class SignalPinReminderSchedule implements MegaphoneSchedule {

  @Override
  public boolean shouldDisplay(int seenCount, long lastSeen, long firstVisible, long currentTime) {
    if (SignalStore.svr().hasOptedOut()) {
      return false;
    }

    if (!SignalStore.svr().hasPin()) {
      return false;
    }

    if (!SignalStore.pin().arePinRemindersEnabled()) {
      return false;
    }

    if (!SignalStore.account().isRegistered()) {
      return false;
    }

    long lastReminderTime = SignalStore.pin().getLastReminderTime();
    long interval         = SignalStore.pin().getCurrentInterval();

    return currentTime - lastReminderTime >= interval;
  }
}
