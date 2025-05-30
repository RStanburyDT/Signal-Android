package org.thoughtcrime.securesms.ryan.components;

import android.Manifest;
import android.content.Context;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.audio.AudioRecordingHandler;
import org.thoughtcrime.securesms.ryan.permissions.Permissions;
import org.thoughtcrime.securesms.ryan.util.ViewUtil;

public final class MicrophoneRecorderView extends FrameLayout implements View.OnTouchListener {

  enum State {
    NOT_RUNNING,
    RUNNING_HELD,
    RUNNING_LOCKED
  }

  public static final int ANIMATION_DURATION = 200;

  private           FloatingRecordButton   floatingRecordButton;
  private           LockDropTarget        lockDropTarget;
  private @Nullable AudioRecordingHandler handler;
  private @NonNull  State                 state = State.NOT_RUNNING;

  private final AudioManager audioManager;

  public MicrophoneRecorderView(Context context) {
    super(context);
    this.audioManager = ContextCompat.getSystemService(context, AudioManager.class);
  }

  public MicrophoneRecorderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.audioManager = ContextCompat.getSystemService(context, AudioManager.class);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    floatingRecordButton = new FloatingRecordButton(getContext(), findViewById(R.id.quick_audio_fab));
    lockDropTarget       = new LockDropTarget      (getContext(), findViewById(R.id.lock_drop_target));

    View recordButton = findViewById(R.id.quick_audio_toggle);
    recordButton.setOnTouchListener(this);
  }

  public void cancelAction(boolean byUser) {
    if (state != State.NOT_RUNNING) {
      state = State.NOT_RUNNING;
      hideUi();

      if (handler != null) {
        handler.onRecordCanceled(byUser);
      }
    }
  }

  public void saveAction() {
    if (state != State.NOT_RUNNING) {
      state = State.NOT_RUNNING;
      hideUi();

      if (handler != null) {
        handler.onRecordSaved();
      }
    }
  }

  public boolean isRecordingLocked() {
    return state == State.RUNNING_LOCKED;
  }

  private void lockAction() {
    if (state == State.RUNNING_HELD) {
      state = State.RUNNING_LOCKED;
      hideUi();

      if (handler != null) handler.onRecordLocked();
    }
  }

  public void unlockAction() {
    if (state == State.RUNNING_LOCKED) {
      state = State.NOT_RUNNING;
      hideUi();

      if (handler != null) handler.onRecordReleased();
    }
  }

  private void hideUi() {
    floatingRecordButton.hide();
    lockDropTarget.hide();
  }

  @Override
  public boolean onTouch(View v, final MotionEvent event) {
    boolean isMicPossiblyInUse = false;
    if (audioManager != null) {
      isMicPossiblyInUse = audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION || audioManager.getMode() == AudioManager.MODE_IN_CALL;
    }
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (!Permissions.hasAll(getContext(), Manifest.permission.RECORD_AUDIO)) {
          if (handler != null) handler.onRecordPermissionRequired();
        } else if (isMicPossiblyInUse) {
          if (handler != null) handler.onRecorderAlreadyInUse();
        } else if (state == State.NOT_RUNNING) {
          state = State.RUNNING_HELD;
          floatingRecordButton.display(event.getX(), event.getY());
          lockDropTarget.display();
          if (handler != null) handler.onRecordPressed();
        }
        break;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (this.state == State.RUNNING_HELD) {
          state = State.NOT_RUNNING;
          hideUi();
          if (handler != null) handler.onRecordReleased();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (this.state == State.RUNNING_HELD) {
          this.floatingRecordButton.moveTo(event.getX(), event.getY());
          if (handler != null) handler.onRecordMoved(floatingRecordButton.lastOffsetX, event.getRawX());

          int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.recording_voice_lock_target);
          if (floatingRecordButton.lastOffsetY <= dimensionPixelSize) {
            lockAction();
          }
        }
        break;
    }

    return false;
  }

  public void setHandler(@Nullable AudioRecordingHandler handler) {
    this.handler = handler;
  }

  private static class FloatingRecordButton {

    private final ImageView recordButtonFab;

    private float startPositionX;
    private float startPositionY;
    private float lastOffsetX;
    private float lastOffsetY;

    FloatingRecordButton(Context context, ImageView recordButtonFab) {
      this.recordButtonFab = recordButtonFab;
      this.recordButtonFab.getBackground().setColorFilter(context.getResources()
                                                                 .getColor(R.color.red_500),
                                                          PorterDuff.Mode.SRC_IN);
    }

    void display(float x, float y) {
      this.startPositionX = x;
      this.startPositionY = y;

      recordButtonFab.setVisibility(View.VISIBLE);

      AnimationSet animation = new AnimationSet(true);
      animation.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0,
                                                    Animation.ABSOLUTE, 0,
                                                    Animation.ABSOLUTE, 0,
                                                    Animation.ABSOLUTE, 0));

      animation.addAnimation(new ScaleAnimation(.5f, 1f, .5f, 1f,
                                                Animation.RELATIVE_TO_SELF, .5f,
                                                Animation.RELATIVE_TO_SELF, .5f));

      animation.setDuration(ANIMATION_DURATION);
      animation.setInterpolator(new OvershootInterpolator());

      recordButtonFab.startAnimation(animation);
    }

    void moveTo(float x, float y) {
      lastOffsetX   = getXOffset(x);
      lastOffsetY   = getYOffset(y);

      if (Math.abs(lastOffsetX) > Math.abs(lastOffsetY)) {
        lastOffsetY = 0;
      } else {
        lastOffsetX = 0;
      }

      recordButtonFab.setTranslationX(lastOffsetX);
      recordButtonFab.setTranslationY(lastOffsetY);
    }

    void hide() {
      recordButtonFab.setTranslationX(0);
      recordButtonFab.setTranslationY(0);
      if (recordButtonFab.getVisibility() != VISIBLE) return;

      AnimationSet animation = new AnimationSet(false);
      Animation scaleAnimation = new ScaleAnimation(1, 0.5f, 1, 0.5f,
                                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                                    Animation.RELATIVE_TO_SELF, 0.5f);

      Animation translateAnimation = new TranslateAnimation(Animation.ABSOLUTE, lastOffsetX,
                                                            Animation.ABSOLUTE, 0,
                                                            Animation.ABSOLUTE, lastOffsetY,
                                                            Animation.ABSOLUTE, 0);

      scaleAnimation.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
      translateAnimation.setInterpolator(new DecelerateInterpolator());
      animation.addAnimation(scaleAnimation);
      animation.addAnimation(translateAnimation);
      animation.setDuration(ANIMATION_DURATION);
      animation.setInterpolator(new AnticipateOvershootInterpolator(1.5f));

      recordButtonFab.setVisibility(View.GONE);
      recordButtonFab.clearAnimation();
      recordButtonFab.startAnimation(animation);
    }

    private float getXOffset(float x) {
      return ViewUtil.isLtr(recordButtonFab) ? -Math.max(0, this.startPositionX - x)
                                             : Math.max(0, x - this.startPositionX);
    }

    private float getYOffset(float y) {
      return Math.min(0, y - this.startPositionY);
    }
  }

  private static class LockDropTarget {

    private final View lockDropTarget;
    private final int  dropTargetPosition;

    LockDropTarget(Context context, View lockDropTarget) {
      this.lockDropTarget     = lockDropTarget;
      this.dropTargetPosition = context.getResources().getDimensionPixelSize(R.dimen.recording_voice_lock_target);
    }

    void display() {
      lockDropTarget.setScaleX(1);
      lockDropTarget.setScaleY(1);
      lockDropTarget.setAlpha(0);
      lockDropTarget.setTranslationY(0);
      lockDropTarget.setVisibility(VISIBLE);
      lockDropTarget.animate()
                    .setStartDelay(ANIMATION_DURATION * 2)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .translationY(dropTargetPosition)
                    .alpha(1)
                    .start();
    }

    void hide() {
      lockDropTarget.animate()
                    .setStartDelay(0)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new LinearInterpolator())
                    .scaleX(0).scaleY(0)
                    .start();
    }
  }
}
