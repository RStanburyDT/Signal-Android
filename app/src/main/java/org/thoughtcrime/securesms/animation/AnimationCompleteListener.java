package org.thoughtcrime.securesms.ryan.animation;


import android.animation.Animator;

public abstract class AnimationCompleteListener implements Animator.AnimatorListener {
  @Override
  public final void onAnimationStart(Animator animation) {}

  @Override
  public abstract void onAnimationEnd(Animator animation);

  @Override
  public void onAnimationCancel(Animator animation) {}
  @Override
  public final void onAnimationRepeat(Animator animation) {}
}
