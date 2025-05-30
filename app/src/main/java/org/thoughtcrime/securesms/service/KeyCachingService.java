/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.ryan.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.BuildConfig;
import org.thoughtcrime.securesms.ryan.DummyActivity;
import org.thoughtcrime.securesms.ryan.MainActivity;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.crypto.InvalidPassphraseException;
import org.thoughtcrime.securesms.ryan.crypto.MasterSecret;
import org.thoughtcrime.securesms.ryan.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore;
import org.thoughtcrime.securesms.ryan.migrations.ApplicationMigrations;
import org.thoughtcrime.securesms.ryan.notifications.NotificationChannels;
import org.thoughtcrime.securesms.ryan.util.AppForegroundObserver;
import org.thoughtcrime.securesms.ryan.util.DynamicLanguage;
import org.thoughtcrime.securesms.ryan.util.ServiceUtil;

import java.util.concurrent.TimeUnit;

/**
 * Small service that stays running to keep a key cached in memory.
 *
 * @author Moxie Marlinspike
 */

public class KeyCachingService extends Service {

  private static final String TAG = Log.tag(KeyCachingService.class);

  public static final int SERVICE_RUNNING_ID = 4141;

  public  static final String KEY_PERMISSION           = BuildConfig.APPLICATION_ID + ".ACCESS_SECRETS";
  public  static final String NEW_KEY_EVENT            = BuildConfig.APPLICATION_ID + ".service.action.NEW_KEY_EVENT";
  public  static final String CLEAR_KEY_EVENT          = BuildConfig.APPLICATION_ID + ".service.action.CLEAR_KEY_EVENT";
  public  static final String LOCK_TOGGLED_EVENT       = BuildConfig.APPLICATION_ID + ".service.action.LOCK_ENABLED_EVENT";
  private static final String PASSPHRASE_EXPIRED_EVENT = BuildConfig.APPLICATION_ID + ".service.action.PASSPHRASE_EXPIRED_EVENT";
  public  static final String CLEAR_KEY_ACTION         = BuildConfig.APPLICATION_ID + ".service.action.CLEAR_KEY";
  public  static final String DISABLE_ACTION           = BuildConfig.APPLICATION_ID + ".service.action.DISABLE";
  public  static final String LOCALE_CHANGE_EVENT      = BuildConfig.APPLICATION_ID + ".service.action.LOCALE_CHANGE_EVENT";

  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private final IBinder binder  = new KeySetBinder();

  private static MasterSecret masterSecret;

  public KeyCachingService() {}

  public static synchronized boolean isLocked(Context context) {
    boolean locked = masterSecret == null && (!SignalStore.settings().getPassphraseDisabled() || SignalStore.settings().getScreenLockEnabled());

    if (locked) {
      Log.d(TAG, "Locked! PasswordDisabled: " + SignalStore.settings().getPassphraseDisabled() + ", ScreenLock: " + SignalStore.settings().getScreenLockEnabled());
    }

    return locked;
  }

  public static synchronized @Nullable MasterSecret getMasterSecret(Context context) {
    if (masterSecret == null && (SignalStore.settings().getPassphraseDisabled() && !SignalStore.settings().getScreenLockEnabled())) {
      try {
        return MasterSecretUtil.getMasterSecret(context, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
      } catch (InvalidPassphraseException e) {
        Log.w(TAG, e);
      }
    }

    return masterSecret;
  }

  public static void onAppForegrounded(@NonNull Context context) {
    if (SignalStore.settings().getScreenLockEnabled() || !SignalStore.settings().getPassphraseDisabled()) {
      ServiceUtil.getAlarmManager(context).cancel(buildExpirationPendingIntent(context));
    }
  }

  public static void onAppBackgrounded(@NonNull Context context) {
    startTimeoutIfAppropriate(context);
  }

  @SuppressLint("StaticFieldLeak")
  public void setMasterSecret(final MasterSecret masterSecret) {
    synchronized (KeyCachingService.class) {
      KeyCachingService.masterSecret = masterSecret;

      foregroundService();
      broadcastNewSecret();
      startTimeoutIfAppropriate(this);

      SignalExecutors.BOUNDED.execute(() -> {
        if (!ApplicationMigrations.isUpdate(KeyCachingService.this)) {
          AppDependencies.getMessageNotifier().updateNotification(KeyCachingService.this);
        }
      });
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) return START_NOT_STICKY;
    Log.d(TAG, "onStartCommand, " + intent.getAction());

    if (intent.getAction() != null) {
      switch (intent.getAction()) {
        case CLEAR_KEY_ACTION:         handleClearKey();        break;
        case PASSPHRASE_EXPIRED_EVENT: handleClearKey();        break;
        case DISABLE_ACTION:           handleDisableService();  break;
        case LOCALE_CHANGE_EVENT:      handleLocaleChanged();   break;
        case LOCK_TOGGLED_EVENT:       handleLockToggled();     break;
      }
    }

    return START_NOT_STICKY;
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate()");
    super.onCreate();

    if (SignalStore.settings().getPassphraseDisabled() && !SignalStore.settings().getScreenLockEnabled()) {
      try {
        MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(this, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
        setMasterSecret(masterSecret);
      } catch (InvalidPassphraseException e) {
        Log.w(TAG, e);
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.w(TAG, "KCS Is Being Destroyed!");
    handleClearKey();
  }

  /**
   * Workaround for Android bug:
   * https://code.google.com/p/android/issues/detail?id=53313
   */
  @Override
  public void onTaskRemoved(Intent rootIntent) {
    Intent intent = new Intent(this, DummyActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleClearKey() {
    Log.i(TAG, "handleClearKey()");
    KeyCachingService.masterSecret = null;
    stopForeground(true);

    Intent intent = new Intent(CLEAR_KEY_EVENT);
    intent.setPackage(getApplicationContext().getPackageName());

    sendBroadcast(intent, KEY_PERMISSION);

    SignalExecutors.BOUNDED.execute(() -> {
      AppDependencies.getMessageNotifier().updateNotification(KeyCachingService.this);
    });
  }

  private void handleLockToggled() {
    stopForeground(true);

    try {
      MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(this, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
      setMasterSecret(masterSecret);
    } catch (InvalidPassphraseException e) {
      Log.w(TAG, e);
    }
  }

  private void handleDisableService() {
    if (SignalStore.settings().getPassphraseDisabled() &&
        !SignalStore.settings().getScreenLockEnabled())
    {
      stopForeground(true);
    }
  }

  private void handleLocaleChanged() {
    dynamicLanguage.updateServiceLocale(this);
    foregroundService();
  }

  private static void startTimeoutIfAppropriate(@NonNull Context context) {
    boolean appVisible       = AppForegroundObserver.isForegrounded();
    boolean secretSet        = KeyCachingService.masterSecret != null;

    boolean timeoutEnabled   = SignalStore.settings().getPassphraseTimeoutEnabled();
    boolean passLockActive   = timeoutEnabled && !SignalStore.settings().getPassphraseDisabled();

    long    screenTimeout       = SignalStore.settings().getScreenLockTimeout();
    boolean screenLockActive    = SignalStore.settings().getScreenLockEnabled();
    boolean immediateScreenLock = screenTimeout == 0 && screenLockActive;

    if (immediateScreenLock) {
      Log.i(TAG, "Default immediate screen lock to one minute");
      SignalStore.settings().setScreenLockTimeout(60);
      immediateScreenLock = false;
    }

    if (!appVisible && secretSet && (passLockActive || screenLockActive)) {
      if (immediateScreenLock) {
        Log.i(TAG, "Starting immediate screen lock");
        Intent intent = new Intent(PASSPHRASE_EXPIRED_EVENT, null, context, KeyCachingService.class);
        context.startService(intent);
      } else {
        long passphraseTimeoutMinutes = SignalStore.settings().getPassphraseTimeout();
        long screenLockTimeoutSeconds = SignalStore.settings().getScreenLockTimeout();

        long timeoutMillis;

        if (!SignalStore.settings().getPassphraseDisabled()) timeoutMillis = TimeUnit.MINUTES.toMillis(passphraseTimeoutMinutes);
        else                                                    timeoutMillis = TimeUnit.SECONDS.toMillis(screenLockTimeoutSeconds);

        Log.i(TAG, "Starting timeout: " + timeoutMillis);

        AlarmManager  alarmManager     = ServiceUtil.getAlarmManager(context);
        PendingIntent expirationIntent = buildExpirationPendingIntent(context);

        alarmManager.cancel(expirationIntent);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + timeoutMillis, expirationIntent);
      }
    }
  }

  private void foregroundService() {
    if (SignalStore.settings().getPassphraseDisabled() && !SignalStore.settings().getScreenLockEnabled()) {
      stopForeground(true);
      return;
    }

    Log.i(TAG, "foregrounding KCS");
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationChannels.getInstance().LOCKED_STATUS);

    builder.setContentTitle(getString(R.string.KeyCachingService_passphrase_cached));
    builder.setContentText(getString(R.string.KeyCachingService_signal_passphrase_cached));
    builder.setSmallIcon(R.drawable.ic_notification_unlocked);
    builder.setColor(ContextCompat.getColor(this, R.color.signal_light_colorSecondary));
    builder.setWhen(0);
    builder.setPriority(Notification.PRIORITY_MIN);
    builder.setOngoing(true);

    builder.addAction(R.drawable.symbol_lock_24, getString(R.string.KeyCachingService_lock), buildLockIntent());
    builder.setContentIntent(buildLaunchIntent());

    stopForeground(true);
    startForeground(SERVICE_RUNNING_ID, builder.build());
  }

  private void broadcastNewSecret() {
    Log.i(TAG, "Broadcasting new secret...");

    Intent intent = new Intent(NEW_KEY_EVENT);
    intent.setPackage(getApplicationContext().getPackageName());

    sendBroadcast(intent, KEY_PERMISSION);
  }

  private PendingIntent buildLockIntent() {
    Intent intent = new Intent(this, KeyCachingService.class);
    intent.setAction(PASSPHRASE_EXPIRED_EVENT);
    return PendingIntent.getService(getApplicationContext(), 0, intent, getPendingIntentFlags());
  }

  private PendingIntent buildLaunchIntent() {
    // TODO [greyson] Navigation
    return PendingIntent.getActivity(getApplicationContext(), 0, MainActivity.clearTop(this), getPendingIntentFlags());
  }

  private static PendingIntent buildExpirationPendingIntent(@NonNull Context context) {
    Intent expirationIntent = new Intent(PASSPHRASE_EXPIRED_EVENT, null, context, KeyCachingService.class);
    return PendingIntent.getService(context, 0, expirationIntent, getPendingIntentFlags());
  }

  private static int getPendingIntentFlags() {
    if (Build.VERSION.SDK_INT >= 23) {
      return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
    } else {
      return PendingIntent.FLAG_UPDATE_CURRENT;
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return binder;
  }

  public class KeySetBinder extends Binder {
    public KeyCachingService getService() {
      return KeyCachingService.this;
    }
  }
}
