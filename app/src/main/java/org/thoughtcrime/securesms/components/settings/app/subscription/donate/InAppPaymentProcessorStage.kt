package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.donate

enum class InAppPaymentProcessorStage {
  INIT,
  PAYMENT_PIPELINE,
  CANCELLING,
  FAILED,
  COMPLETE;

  val isInProgress: Boolean get() = this == PAYMENT_PIPELINE || this == CANCELLING
  val isTerminal: Boolean get() = this == FAILED || this == COMPLETE
}
