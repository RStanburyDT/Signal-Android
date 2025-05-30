/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.banner.banners

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.SignalPreview
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.banner.Banner
import org.thoughtcrime.securesms.ryan.banner.ui.compose.Action
import org.thoughtcrime.securesms.ryan.banner.ui.compose.DefaultBanner
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore

class BubbleOptOutBanner(private val inBubble: Boolean, private val actionListener: (Boolean) -> Unit) : Banner<Unit>() {

  override val enabled: Boolean
    get() = inBubble && !SignalStore.tooltips.hasSeenBubbleOptOutTooltip() && Build.VERSION.SDK_INT > 29

  override val dataFlow: Flow<Unit>
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) = Banner(contentPadding, actionListener)
}

@Composable
private fun Banner(contentPadding: PaddingValues, actionListener: (Boolean) -> Unit = {}) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.BubbleOptOutTooltip__description),
    actions = listOf(
      Action(R.string.BubbleOptOutTooltip__turn_off) {
        actionListener(true)
      },
      Action(R.string.BubbleOptOutTooltip__not_now) {
        actionListener(false)
      }
    ),
    paddingValues = contentPadding
  )
}

@SignalPreview
@Composable
private fun BannerPreview() {
  Previews.Preview {
    Banner(PaddingValues(0.dp))
  }
}
