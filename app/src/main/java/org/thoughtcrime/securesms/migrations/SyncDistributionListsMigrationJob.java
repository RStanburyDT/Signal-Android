package org.thoughtcrime.securesms.ryan.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.jobmanager.Job;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;
import org.thoughtcrime.securesms.ryan.storage.StorageSyncHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Marks all distribution lists as needing to be synced with storage service.
 */
public final class SyncDistributionListsMigrationJob extends MigrationJob {

  public static final String KEY = "SyncDistributionListsMigrationJob";

  private static final String TAG = Log.tag(SyncDistributionListsMigrationJob.class);

  SyncDistributionListsMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private SyncDistributionListsMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public boolean isUiBlocking() {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void performMigration() {
    if (SignalStore.account().getAci() == null) {
      Log.w(TAG, "Self not yet available.");
      return;
    }

    List<RecipientId> listRecipients = SignalDatabase.distributionLists()
                                                     .getAllListRecipients()
                                                     .stream()
                                                     .filter(id -> {
                                                       try {
                                                         Recipient.resolved(id);
                                                         return true;
                                                       } catch (Exception e) {
                                                         Log.e(TAG, "Unable to resolve distribution list recipient: " + id, e);
                                                         return false;
                                                       }
                                                     })
                                                     .collect(Collectors.toList());

    SignalDatabase.recipients().markNeedsSync(listRecipients);
    StorageSyncHelper.scheduleSyncForDataChange();
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<SyncDistributionListsMigrationJob> {
    @Override
    public @NonNull SyncDistributionListsMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new SyncDistributionListsMigrationJob(parameters);
    }
  }
}
