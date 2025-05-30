package org.thoughtcrime.securesms.ryan.components.settings.app.privacy.screenlock

/**
 * Information about the screen lock state. Used in [ScreenLockSettingsViewModel].
 */
data class ScreenLockSettingsState(
  val screenLock: Boolean = false,
  val screenLockActivityTimeout: Long = 0L
)
