package org.thoughtcrime.securesms.ryan.util;

import android.text.Html;

import androidx.annotation.NonNull;

public class HtmlUtil {
  public static @NonNull String bold(@NonNull String target) {
    return "<b>" + Html.escapeHtml(target) + "</b>";
  }
}
