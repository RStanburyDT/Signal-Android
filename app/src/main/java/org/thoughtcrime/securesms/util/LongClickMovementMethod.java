package org.thoughtcrime.securesms.ryan.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.components.spoiler.SpoilerAnnotation;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;

import java.lang.ref.WeakReference;

public class LongClickMovementMethod extends LinkMovementMethod {
  @SuppressLint("StaticFieldLeak")
  private static LongClickMovementMethod sInstance;

  private final GestureDetector     gestureDetector;
  private       WeakReference<View> widget;
  private       LongClickCopySpan   currentSpan;

  private LongClickMovementMethod(final Context context) {
    gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public void onLongPress(MotionEvent e) {
        if (currentSpan != null && widget != null && widget.get() != null) {
          currentSpan.onLongClick(widget.get());
          widget = null;
          currentSpan = null;
        }
      }

      @Override
      public boolean onSingleTapUp(MotionEvent e) {
        if (currentSpan != null && widget != null && widget.get() != null) {
          currentSpan.onClick(widget.get());
          widget = null;
          currentSpan = null;
        }
        return true;
      }
    });
  }

  @Override
  public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
    int action = event.getAction();

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
      int x = (int) event.getX();
      int y = (int) event.getY();

      x -= widget.getTotalPaddingLeft();
      y -= widget.getTotalPaddingTop();

      x += widget.getScrollX();
      y += widget.getScrollY();

      Layout layout = widget.getLayout();
      int line = layout.getLineForVertical(y);
      int off = layout.getOffsetForHorizontal(line, x);

      SpoilerAnnotation.SpoilerClickableSpan[] spoilerClickableSpans = buffer.getSpans(off, off, SpoilerAnnotation.SpoilerClickableSpan.class);
      if (spoilerClickableSpans.length != 0) {
        boolean spoilerRevealed = false;
        for (SpoilerAnnotation.SpoilerClickableSpan spoilerClickSpan : spoilerClickableSpans) {
          if (!spoilerClickSpan.getSpoilerRevealed() && action == MotionEvent.ACTION_DOWN) {
            return true;
          }

          if (!spoilerClickSpan.getSpoilerRevealed() && action == MotionEvent.ACTION_UP) {
            spoilerClickSpan.onClick(widget);
            spoilerRevealed = true;
          }
        }

        if (spoilerRevealed) {
          return true;
        }
      }

      LongClickCopySpan[] longClickCopySpan = buffer.getSpans(off, off, LongClickCopySpan.class);
      if (longClickCopySpan.length != 0) {
        LongClickCopySpan aSingleSpan = longClickCopySpan[0];
        if (action == MotionEvent.ACTION_DOWN) {
          Selection.setSelection(buffer, buffer.getSpanStart(aSingleSpan), buffer.getSpanEnd(aSingleSpan));
          aSingleSpan.setHighlighted(true, ContextCompat.getColor(widget.getContext(), R.color.touch_highlight));
        } else {
          Selection.removeSelection(buffer);
          aSingleSpan.setHighlighted(false, Color.TRANSPARENT);
        }

        this.currentSpan = aSingleSpan;
        this.widget = new WeakReference<>(widget);
        return gestureDetector.onTouchEvent(event);
      } else if (action == MotionEvent.ACTION_UP && Selection.getSelectionEnd(buffer) > 0){
        Selection.setSelection(buffer, 0);
      }
    } else if (action == MotionEvent.ACTION_CANCEL) {
      // Remove Selections.
      LongClickCopySpan[] spans = buffer.getSpans(Selection.getSelectionStart(buffer), Selection.getSelectionEnd(buffer), LongClickCopySpan.class);
      for (LongClickCopySpan aSpan : spans) {
        aSpan.setHighlighted(false, Color.TRANSPARENT);
      }
      Selection.removeSelection(buffer);
      return gestureDetector.onTouchEvent(event);
    }
    return super.onTouchEvent(widget, buffer, event);
  }

  /** This signature is available in the base class and can lead to the wrong instance being returned. */
  public static LongClickMovementMethod getInstance() {
    return getInstance(AppDependencies.getApplication());
  }

  public static LongClickMovementMethod getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new LongClickMovementMethod(context.getApplicationContext());
    }
    return sInstance;
  }
}