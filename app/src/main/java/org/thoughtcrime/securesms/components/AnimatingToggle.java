package org.thoughtcrime.securesms.ryan.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.ViewUtil;

public class AnimatingToggle extends FrameLayout {

  private View current;
  private View previous;
  private final Animation inAnimation;
  private final Animation outAnimation;

  public AnimatingToggle(Context context) {
    this(context, null);
  }

  public AnimatingToggle(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AnimatingToggle(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.outAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_out);
    this.inAnimation  = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_in);
    this.outAnimation.setInterpolator(new FastOutSlowInInterpolator());
    this.inAnimation.setInterpolator(new FastOutSlowInInterpolator());
  }

  @Override
  public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    if (!isInEditMode()) {
      if (getChildCount() == 1) {
        current = child;
        child.setVisibility(View.VISIBLE);
      } else {
        child.setVisibility(View.GONE);
      }
      child.setClickable(false);
    }
  }

  public void display(@Nullable View view) {
    if (view == current && current.getVisibility() == View.VISIBLE) return;
    if (previous != null && previous.getAnimation() == outAnimation) {
      previous.clearAnimation();
      previous.setVisibility(View.GONE);
    }
    if (current != null) {
      ViewUtil.animateOut(current, outAnimation, View.GONE);
    }
    if (view != null) {
      ViewUtil.animateIn(view, inAnimation);
    }
    previous = current;
    current = view;
  }

  public void displayQuick(@Nullable View view) {
    if (view == current && current.getVisibility() == View.VISIBLE) return;
    if (current != null) current.setVisibility(View.GONE);
    if (view != null) {
      view.setVisibility(View.VISIBLE);
      view.clearAnimation();
    }
    current = view;
  }
}
