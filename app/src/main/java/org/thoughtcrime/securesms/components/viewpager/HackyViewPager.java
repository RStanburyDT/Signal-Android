package org.thoughtcrime.securesms.ryan.components.viewpager;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

import org.signal.core.util.logging.Log;

/**
 * Hacky fix for http://code.google.com/p/android/issues/detail?id=18990
 * <p/>
 * ScaleGestureDetector seems to mess up the touch events, which means that
 * ViewGroups which make use of onInterceptTouchEvent throw a lot of
 * IllegalArgumentException: pointerIndex out of range.
 * <p/>
 * There's not much I can do in my code for now, but we can mask the result by
 * just catching the problem and ignoring it.
 *
 * @author Chris Banes
 */
public class HackyViewPager extends ViewPager {

  private static final String TAG = Log.tag(HackyViewPager.class);

  public HackyViewPager(Context context) {
    super(context);
  }

  public HackyViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    try {
      return super.onInterceptTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, e);
      return false;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    try {
      return super.onTouchEvent(ev);
    } catch (IndexOutOfBoundsException | NullPointerException e) {
      Log.w(TAG, e);
      return true;
    }
  }
}