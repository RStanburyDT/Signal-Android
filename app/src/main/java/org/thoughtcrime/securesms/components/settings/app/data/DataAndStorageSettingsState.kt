package org.thoughtcrime.securesms.ryan.components.settings.app.data

import org.thoughtcrime.securesms.ryan.mms.SentMediaQuality
import org.thoughtcrime.securesms.ryan.webrtc.CallDataMode

data class DataAndStorageSettingsState(
  val totalStorageUse: Long,
  val mobileAutoDownloadValues: Set<String>,
  val wifiAutoDownloadValues: Set<String>,
  val roamingAutoDownloadValues: Set<String>,
  val callDataMode: CallDataMode,
  val isProxyEnabled: Boolean,
  val sentMediaQuality: SentMediaQuality
)
