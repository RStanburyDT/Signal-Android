package org.thoughtcrime.securesms.ryan.util;

import androidx.annotation.StyleRes;

import org.thoughtcrime.securesms.ryan.R;

public class DynamicNoActionBarInviteTheme extends DynamicTheme {

  protected @StyleRes int getTheme() {
    return R.style.Signal_DayNight_Invite;
  }
}
