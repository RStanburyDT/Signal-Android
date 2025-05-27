/**
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.calls.links.details

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.FragmentWrapperActivity
import org.thoughtcrime.securesms.ryan.service.webrtc.links.CallLinkRoomId

class CallLinkDetailsActivity : FragmentWrapperActivity() {
  override fun getFragment(): Fragment = NavHostFragment.create(R.navigation.call_link_details, intent.extras!!.getBundle(BUNDLE))

  companion object {

    private const val BUNDLE = "bundle"

    fun createIntent(context: Context, callLinkRoomId: CallLinkRoomId): Intent {
      return Intent(context, CallLinkDetailsActivity::class.java)
        .putExtra(BUNDLE, CallLinkDetailsFragmentArgs.Builder(callLinkRoomId).build().toBundle())
    }
  }
}
