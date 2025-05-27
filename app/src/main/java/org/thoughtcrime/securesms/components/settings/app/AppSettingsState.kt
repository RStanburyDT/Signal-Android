package org.thoughtcrime.securesms.ryan.components.settings.app

import androidx.compose.runtime.Immutable
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.util.Environment
import org.thoughtcrime.securesms.ryan.util.RemoteConfig

@Immutable
data class AppSettingsState(
  val unreadPaymentsCount: Int,
  val hasExpiredGiftBadge: Boolean,
  val allowUserToGoToDonationManagementScreen: Boolean,
  val userUnregistered: Boolean,
  val clientDeprecated: Boolean,
  val showInternalPreferences: Boolean = RemoteConfig.internalUser,
  val showPayments: Boolean = SignalStore.payments.paymentsAvailability.showPaymentsMenu(),
  val showAppUpdates: Boolean = Environment.IS_NIGHTLY,
  val showBackups: Boolean = RemoteConfig.messageBackups,
  val backupFailureState: BackupFailureState = BackupFailureState.NONE
) {
  fun isRegisteredAndUpToDate(): Boolean {
    return !userUnregistered && !clientDeprecated
  }
}
