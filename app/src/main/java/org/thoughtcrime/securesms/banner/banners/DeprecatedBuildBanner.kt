/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.banner.banners

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import org.thoughtcrime.securesms.ryan.banner.ui.compose.Importance
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.util.PlayStoreUtil

/**
 * Shown when a build is actively deprecated and unable to connect to the service.
 */
class DeprecatedBuildBanner : Banner<Unit>() {

  override val enabled: Boolean
    get() = SignalStore.misc.isClientDeprecated

  override val dataFlow: Flow<Unit>
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
    Banner(
      contentPadding = contentPadding,
      onUpdateClicked = {
        PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(context)
      }
    )
  }
}

@Composable
private fun Banner(contentPadding: PaddingValues, onUpdateClicked: () -> Unit = {}) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.ExpiredBuildReminder_this_version_of_signal_has_expired),
    importance = Importance.ERROR,
    actions = listOf(
      Action(R.string.ExpiredBuildReminder_update_now) {
        onUpdateClicked()
      }
    ),
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
