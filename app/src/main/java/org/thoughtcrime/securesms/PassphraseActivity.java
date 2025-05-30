/**
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
package org.thoughtcrime.securesms.ryan;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.crypto.MasterSecret;
import org.thoughtcrime.securesms.ryan.service.KeyCachingService;


/**
 * Base Activity for changing/prompting local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */
public abstract class PassphraseActivity extends BaseActivity {

  private static final String TAG = Log.tag(PassphraseActivity.class);

  private KeyCachingService keyCachingService;
  private MasterSecret masterSecret;

  protected void setMasterSecret(MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
    Intent bindIntent = new Intent(this, KeyCachingService.class);
    startService(bindIntent);
    bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  protected abstract void cleanup();

  private ServiceConnection serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        keyCachingService = ((KeyCachingService.KeySetBinder)service).getService();
        keyCachingService.setMasterSecret(masterSecret);

        PassphraseActivity.this.unbindService(PassphraseActivity.this.serviceConnection);

        masterSecret = null;
        cleanup();

        Intent nextIntent = getIntent().getParcelableExtra("next_intent");
        if (nextIntent != null) {
            try {
                startActivity(nextIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } catch (java.lang.SecurityException e) {
                Log.w(TAG, "Access permission not passed from PassphraseActivity, retry sharing.");
            }
        }
        finish();
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        keyCachingService = null;
      }
  };
}
