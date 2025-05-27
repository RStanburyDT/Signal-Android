package org.thoughtcrime.securesms.ryan.pin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.ryan.MainActivity;
import org.thoughtcrime.securesms.ryan.PassphraseRequiredActivity;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.lock.v2.CreateSvrPinActivity;
import org.thoughtcrime.securesms.ryan.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.ryan.util.DynamicTheme;

public final class PinRestoreActivity extends AppCompatActivity {

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pin_restore_activity);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }

  void navigateToPinCreation() {
    final Intent main      = MainActivity.clearTop(this);
    final Intent createPin = CreateSvrPinActivity.getIntentForPinCreate(this);
    final Intent chained   = PassphraseRequiredActivity.chainIntent(createPin, main);

    startActivity(chained);
    finish();
  }
}
