package org.thoughtcrime.securesms.ryan.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import org.thoughtcrime.securesms.ryan.R;

public class CircleColorImageView extends AppCompatImageView {

 public CircleColorImageView(Context context) {
    this(context, null);
  }

  public CircleColorImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CircleColorImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    int circleColor = Color.WHITE;

    if (attrs != null) {
      TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleColorImageView, 0, 0);
      circleColor = typedArray.getColor(R.styleable.CircleColorImageView_circleColor, Color.WHITE);
      typedArray.recycle();
    }

    Drawable circle = context.getResources().getDrawable(R.drawable.circle_tintable);
    circle.setColorFilter(circleColor, PorterDuff.Mode.SRC_IN);

    setBackgroundDrawable(circle);
  }
}
