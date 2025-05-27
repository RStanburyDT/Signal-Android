package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.receipts.detail

import org.thoughtcrime.securesms.ryan.database.model.InAppPaymentReceiptRecord

data class DonationReceiptDetailState(
  val inAppPaymentReceiptRecord: InAppPaymentReceiptRecord? = null
)
