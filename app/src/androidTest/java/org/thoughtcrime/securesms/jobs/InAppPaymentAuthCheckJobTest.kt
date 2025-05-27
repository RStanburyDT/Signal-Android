package org.thoughtcrime.securesms.ryan.jobs

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.signal.core.util.deleteAll
import org.signal.core.util.money.FiatMoney
import org.signal.donations.InAppPaymentType
import org.signal.donations.json.StripeIntentStatus
import org.signal.donations.json.StripePaymentIntent
import org.thoughtcrime.securesms.ryan.components.settings.app.subscription.DonationSerializationHelper.toFiatValue
import org.thoughtcrime.securesms.ryan.database.DonationReceiptTable
import org.thoughtcrime.securesms.ryan.database.InAppPaymentTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.InAppPaymentReceiptRecord
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.InAppPaymentData
import org.thoughtcrime.securesms.ryan.dependencies.InstrumentationApplicationDependencyProvider
import org.thoughtcrime.securesms.ryan.testing.Get
import org.thoughtcrime.securesms.ryan.testing.SignalActivityRule
import org.thoughtcrime.securesms.ryan.testing.success
import org.thoughtcrime.securesms.ryan.util.TestStripePaths
import java.math.BigDecimal
import java.util.Currency

@RunWith(AndroidJUnit4::class)
class InAppPaymentAuthCheckJobTest {

  companion object {
    private const val TEST_INTENT_ID = "test-intent-id"
    private const val TEST_CLIENT_SECRET = "test-client-secret"
  }

  @get:Rule
  val harness = SignalActivityRule()

  @Before
  fun setUp() {
    SignalDatabase.inAppPayments.writableDatabase.deleteAll(InAppPaymentTable.TABLE_NAME)
    SignalDatabase.donationReceipts.writableDatabase.deleteAll(DonationReceiptTable.TABLE_NAME)
  }

  @Test
  fun givenCanceledOneTimeAuthRequiredPayment_whenICheck_thenIDoNotExpectAReceipt() {
    initializeMockGetPaymentIntent(status = StripeIntentStatus.CANCELED)

    SignalDatabase.inAppPayments.insert(
      type = InAppPaymentType.ONE_TIME_DONATION,
      state = InAppPaymentTable.State.WAITING_FOR_AUTHORIZATION,
      subscriberId = null,
      endOfPeriod = null,
      inAppPaymentData = InAppPaymentData(
        amount = FiatMoney(BigDecimal.ONE, Currency.getInstance("USD")).toFiatValue(),
        waitForAuth = InAppPaymentData.WaitingForAuthorizationState(
          stripeIntentId = TEST_INTENT_ID,
          stripeClientSecret = TEST_CLIENT_SECRET
        )
      )
    )

    InAppPaymentAuthCheckJob().run()

    val receipts = SignalDatabase.donationReceipts.getReceipts(InAppPaymentReceiptRecord.Type.ONE_TIME_DONATION)
    assertThat(receipts).isEmpty()
  }

  private fun initializeMockGetPaymentIntent(status: StripeIntentStatus) {
    InstrumentationApplicationDependencyProvider.addMockWebRequestHandlers(
      Get(TestStripePaths.getPaymentIntentPath(TEST_INTENT_ID, TEST_CLIENT_SECRET)) {
        MockResponse().success(
          StripePaymentIntent(
            id = TEST_INTENT_ID,
            clientSecret = TEST_CLIENT_SECRET,
            status = status,
            paymentMethod = null
          )
        )
      }
    )
  }
}
