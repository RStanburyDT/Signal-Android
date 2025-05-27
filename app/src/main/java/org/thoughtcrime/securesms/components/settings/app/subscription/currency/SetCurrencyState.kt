package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.currency

import java.util.Currency

data class SetCurrencyState(
  val selectedCurrencyCode: String = "",
  val currencies: List<Currency> = listOf()
)
