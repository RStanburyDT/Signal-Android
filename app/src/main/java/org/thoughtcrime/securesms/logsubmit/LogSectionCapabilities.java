package org.thoughtcrime.securesms.ryan.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.AppCapabilities;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.database.model.RecipientRecord;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.util.RemoteConfig;
import org.whispersystems.signalservice.api.account.AccountAttributes;

public final class LogSectionCapabilities implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "CAPABILITIES";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    if (!SignalStore.account().isRegistered()) {
      return "Unregistered";
    }

    if (SignalStore.account().getE164() == null || SignalStore.account().getAci() == null) {
      return "Self not yet available!";
    }

    Recipient self = Recipient.self();

    AccountAttributes.Capabilities localCapabilities  = AppCapabilities.getCapabilities(false);
    RecipientRecord.Capabilities   globalCapabilities = SignalDatabase.recipients().getCapabilities(self.getId());

    StringBuilder builder = new StringBuilder().append("-- Local").append("\n")
                                               .append("DeleteSync: ").append(localCapabilities.getDeleteSync()).append("\n")
                                               .append("VersionedExpirationTimer: ").append(localCapabilities.getVersionedExpirationTimer()).append("\n")
                                               .append("StorageServiceEncryptionV2: ").append(localCapabilities.getStorageServiceEncryptionV2()).append("\n")
                                               .append("\n")
                                               .append("-- Global").append("\n");

    if (globalCapabilities != null) {
      builder.append("StorageServiceEncryptionV2: ").append(globalCapabilities.getStorageServiceEncryptionV2()).append("\n");
      builder.append("\n");
    } else {
      builder.append("Self not found!");
    }

    return builder;
  }
}
