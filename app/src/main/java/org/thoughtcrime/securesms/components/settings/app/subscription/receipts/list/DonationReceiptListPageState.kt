package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.receipts.list

import org.thoughtcrime.securesms.ryan.database.model.InAppPaymentReceiptRecord

data class DonationReceiptListPageState(
  val records: List<InAppPaymentReceiptRecord> = emptyList(),
  val isLoaded: Boolean = false
)
