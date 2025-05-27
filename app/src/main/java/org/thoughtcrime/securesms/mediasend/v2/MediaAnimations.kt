package org.thoughtcrime.securesms.ryan.mediasend.v2

import android.view.animation.Interpolator
import org.thoughtcrime.securesms.ryan.util.createDefaultCubicBezierInterpolator

object MediaAnimations {
  /**
   * Fast-In-Extra-Slow-Out Interpolator
   */
  @JvmStatic
  val interpolator: Interpolator = createDefaultCubicBezierInterpolator()
}
