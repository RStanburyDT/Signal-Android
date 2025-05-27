package org.thoughtcrime.securesms.ryan.components.settings.app.account

data class AccountSettingsState(
  val hasPin: Boolean,
  val hasRestoredAep: Boolean,
  val pinRemindersEnabled: Boolean,
  val registrationLockEnabled: Boolean,
  val userUnregistered: Boolean,
  val clientDeprecated: Boolean
) {
  fun isNotDeprecatedOrUnregistered(): Boolean {
    return !(userUnregistered || clientDeprecated)
  }
}
