/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.settings.app

import androidx.compose.runtime.Immutable
import com.google.common.base.Objects
import org.thoughtcrime.securesms.ryan.badges.models.Badge
import org.thoughtcrime.securesms.ryan.profiles.ProfileName
import org.thoughtcrime.securesms.ryan.recipients.Recipient

/**
 * Derived state class of recipient for BioRow
 */
@Immutable
class BioRecipientState(
  val recipient: Recipient
) {
  val username: String = recipient.username.orElse("")
  val featuredBadge: Badge? = recipient.featuredBadge
  val profileName: ProfileName = recipient.profileName
  val e164: String = recipient.requireE164()
  val combinedAboutAndEmoji: String? = recipient.combinedAboutAndEmoji

  override fun equals(other: Any?): Boolean {
    if (other !is Recipient) return false
    return recipient.hasSameContent(other)
  }

  override fun hashCode(): Int {
    return Objects.hashCode(
      recipient,
      username,
      featuredBadge,
      profileName,
      e164,
      combinedAboutAndEmoji
    )
  }
}
