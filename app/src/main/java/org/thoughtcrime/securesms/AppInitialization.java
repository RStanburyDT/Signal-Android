package org.thoughtcrime.securesms.ryan;

import android.content.Context;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.jobmanager.JobManager;
import org.thoughtcrime.securesms.ryan.jobs.DeleteAbandonedAttachmentsJob;
import org.thoughtcrime.securesms.ryan.jobs.EmojiSearchIndexDownloadJob;
import org.thoughtcrime.securesms.ryan.jobs.StickerPackDownloadJob;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.migrations.ApplicationMigrations;
import org.thoughtcrime.securesms.ryan.stickers.BlessedPacks;
import org.thoughtcrime.securesms.ryan.util.TextSecurePreferences;
import org.thoughtcrime.securesms.ryan.util.Util;

/**
 * Rule of thumb: if there's something you want to do on the first app launch that involves
 * persisting state to the database, you'll almost certainly *also* want to do it post backup
 * restore, since a backup restore will wipe the current state of the database.
 */
public final class AppInitialization {

  private static final String TAG = Log.tag(AppInitialization.class);

  private AppInitialization() {}

  public static void onFirstEverAppLaunch(@NonNull Context context) {
    Log.i(TAG, "onFirstEverAppLaunch()");

    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    SignalStore.settings().setPassphraseDisabled(true);
    TextSecurePreferences.setReadReceiptsEnabled(context, true);
    TextSecurePreferences.setTypingIndicatorsEnabled(context, true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
  }

  public static void onPostBackupRestore(@NonNull Context context) {
    Log.i(TAG, "onPostBackupRestore()");

    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onPostBackupRestore();
    SignalStore.onFirstEverAppLaunch();
    SignalStore.onboarding().clearAll();
    TextSecurePreferences.onPostBackupRestore(context);
    SignalStore.settings().setPassphraseDisabled(true);
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
    EmojiSearchIndexDownloadJob.scheduleImmediately();
    DeleteAbandonedAttachmentsJob.enqueue();
  }

  /**
   * Temporary migration method that does the safest bits of {@link #onFirstEverAppLaunch(Context)}
   */
  public static void onRepairFirstEverAppLaunch(@NonNull Context context) {
    Log.w(TAG, "onRepairFirstEverAppLaunch()");

    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    SignalStore.settings().setPassphraseDisabled(true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.ZOZO.getPackId(), BlessedPacks.ZOZO.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.BANDIT.getPackId(), BlessedPacks.BANDIT.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forInstall(BlessedPacks.DAY_BY_DAY.getPackId(), BlessedPacks.DAY_BY_DAY.getPackKey(), false));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_HANDS.getPackId(), BlessedPacks.SWOON_HANDS.getPackKey()));
    AppDependencies.getJobManager().add(StickerPackDownloadJob.forReference(BlessedPacks.SWOON_FACES.getPackId(), BlessedPacks.SWOON_FACES.getPackKey()));
  }
}
