package org.thoughtcrime.securesms.ryan.components.menu

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.thoughtcrime.securesms.ryan.R

/**
 * Represents an action to be rendered via [SignalContextMenu] or [SignalBottomActionBar]
 */
data class ActionItem @JvmOverloads constructor(
  @DrawableRes val iconRes: Int,
  val title: CharSequence,
  @ColorRes val tintRes: Int = R.color.signal_colorOnSurface,
  val action: Runnable
)
