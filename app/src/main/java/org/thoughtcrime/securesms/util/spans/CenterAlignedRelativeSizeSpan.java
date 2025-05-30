package org.thoughtcrime.securesms.ryan.util.spans;


import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

public class CenterAlignedRelativeSizeSpan extends MetricAffectingSpan {

  private final float relativeSize;

  public CenterAlignedRelativeSizeSpan(float relativeSize) {
    this.relativeSize = relativeSize;
  }

  @Override
  public void updateMeasureState(@NonNull TextPaint p) {
    updateDrawState(p);
  }

  @Override
  public void updateDrawState(TextPaint tp) {
    tp.setTextSize(tp.getTextSize() * relativeSize);
    tp.baselineShift += (int) (tp.ascent() * relativeSize) / 4;
  }
}
