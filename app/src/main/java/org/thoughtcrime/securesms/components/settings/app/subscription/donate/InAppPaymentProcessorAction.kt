package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.donate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class InAppPaymentProcessorAction : Parcelable {
  PROCESS_NEW_IN_APP_PAYMENT,
  UPDATE_SUBSCRIPTION,
  CANCEL_SUBSCRIPTION
}
