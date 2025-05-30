package org.thoughtcrime.securesms.ryan.permissions;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.DimensionUnit;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.ThemeUtil;
import org.thoughtcrime.securesms.ryan.util.ViewUtil;

import java.util.Objects;

public class RationaleDialog {

  public static MaterialAlertDialogBuilder createFor(@NonNull Context context, @NonNull String title, @NonNull String details, @DrawableRes int... drawables) {
    View      view         = LayoutInflater.from(context).inflate(R.layout.permission_allow_dialog, null);
    ViewGroup header       = view.findViewById(R.id.permission_header_container);
    TextView  titleText    = view.findViewById(R.id.permission_title);
    TextView  detailsText  = view.findViewById(R.id.permission_details);
    int       iconSize = (int) DimensionUnit.DP.toPixels(32);

    for (int i = 0; i < drawables.length; i++) {
      Drawable drawable = Objects.requireNonNull(ContextCompat.getDrawable(context, drawables[i]));
      DrawableCompat.setTint(drawable, ContextCompat.getColor(context, R.color.signal_colorOnPrimaryContainer));

      ImageView imageView = new ImageView(context);
      imageView.setImageDrawable(drawable);
      imageView.setLayoutParams(new LayoutParams(iconSize, iconSize));
      imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

      header.addView(imageView);

      if (i != drawables.length - 1) {
        TextView plus = new TextView(context);
        plus.setText("+");
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        plus.setTextColor(Color.WHITE);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(ViewUtil.dpToPx(context, 20), 0, ViewUtil.dpToPx(context, 20), 0);

        plus.setLayoutParams(layoutParams);
        header.addView(plus);
      }
    }

    titleText.setText(title);
    detailsText.setText(details);

    return new MaterialAlertDialogBuilder(context).setView(view);
  }

  public static MaterialAlertDialogBuilder createFor(@NonNull Context context, @NonNull String message, @DrawableRes int... drawables) {
    View      view    = LayoutInflater.from(context).inflate(R.layout.permissions_rationale_dialog, null);
    ViewGroup header  = view.findViewById(R.id.header_container);
    TextView  text    = view.findViewById(R.id.message);
    int       iconSize = (int) DimensionUnit.DP.toPixels(32);

    for (int i=0;i<drawables.length;i++) {
      Drawable drawable = Objects.requireNonNull(ContextCompat.getDrawable(context, drawables[i]));
      DrawableCompat.setTint(drawable, ContextCompat.getColor(context, R.color.white));
      ImageView imageView = new ImageView(context);
      imageView.setImageDrawable(drawable);

      imageView.setLayoutParams(new LayoutParams(iconSize, iconSize));
      imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

      header.addView(imageView);

      if (i != drawables.length - 1) {
        TextView plus = new TextView(context);
        plus.setText("+");
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        plus.setTextColor(Color.WHITE);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(ViewUtil.dpToPx(context, 20), 0, ViewUtil.dpToPx(context, 20), 0);

        plus.setLayoutParams(layoutParams);
        header.addView(plus);
      }
    }

    text.setText(message);
    text.setMovementMethod(new ScrollingMovementMethod());

    return new MaterialAlertDialogBuilder(context,
                                          ThemeUtil.isDarkTheme(context) ? R.style.Theme_Signal_AlertDialog_Dark_Cornered
                                                                         : R.style.Theme_Signal_AlertDialog_Light_Cornered)
        .setView(view);
  }

}
