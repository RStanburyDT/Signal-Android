package org.thoughtcrime.securesms.ryan.components.settings.app.appearance

import org.thoughtcrime.securesms.ryan.keyvalue.SettingsValues

data class AppearanceSettingsState(
  val theme: SettingsValues.Theme,
  val messageFontSize: Int,
  val language: String,
  val isCompactNavigationBar: Boolean
)
