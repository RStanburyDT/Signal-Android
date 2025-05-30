/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.banner.banners

import android.content.Context
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
import org.thoughtcrime.securesms.ryan.banner.ui.compose.DefaultBanner
import org.thoughtcrime.securesms.ryan.banner.ui.compose.Importance
import org.thoughtcrime.securesms.ryan.util.TextSecurePreferences

class ServiceOutageBanner(val context: Context) : Banner<Unit>() {

  override val enabled: Boolean
    get() = TextSecurePreferences.getServiceOutage(context)

  override val dataFlow: Flow<Unit> = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) = Banner(contentPadding)
}

@Composable
private fun Banner(contentPadding: PaddingValues) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.reminder_header_service_outage_text),
    importance = Importance.ERROR,
    paddingValues = contentPadding
  )
}

@SignalPreview
@Composable
private fun BannerPreview() {
  Previews.Preview {
    Banner(contentPadding = PaddingValues(0.dp))
  }
}
