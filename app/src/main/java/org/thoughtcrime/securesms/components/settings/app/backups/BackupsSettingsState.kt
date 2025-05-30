/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.components.settings.app.backups

import org.thoughtcrime.securesms.ryan.backup.v2.MessageBackupTier
import org.thoughtcrime.securesms.ryan.backup.v2.ui.subscription.MessageBackupsType
import kotlin.time.Duration

/**
 * Screen state for top-level backups settings screen.
 */
data class BackupsSettingsState(
  val enabledState: EnabledState = EnabledState.Loading,
  val showBackupTierInternalOverride: Boolean = false,
  val backupTierInternalOverride: MessageBackupTier? = null
) {
  /**
   * Describes the 'enabled' state of backups.
   */
  sealed interface EnabledState {
    /**
     * Loading data for this row
     */
    data object Loading : EnabledState

    /**
     * Google Play Billing is not available on this device
     */
    data object NotAvailable : EnabledState

    /**
     * Backups have never been enabled.
     */
    data object Never : EnabledState

    /**
     * Backups were active at one point, but have been turned off.
     */
    data object Inactive : EnabledState

    /**
     * Backup state couldn't be retrieved from the server for some reason
     */
    data object Failed : EnabledState

    /**
     * Backups are currently active.
     */
    data class Active(val type: MessageBackupsType, val expiresAt: Duration, val lastBackupAt: Duration) : EnabledState
  }
}
