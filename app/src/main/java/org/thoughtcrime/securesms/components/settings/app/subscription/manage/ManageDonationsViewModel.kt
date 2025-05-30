package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.signal.core.util.logging.Log
import org.signal.core.util.orNull
import org.signal.donations.InAppPaymentType
import org.thoughtcrime.securesms.ryan.badges.Badges
import org.thoughtcrime.securesms.ryan.badges.models.Badge
import org.thoughtcrime.securesms.ryan.components.settings.app.subscription.InAppPaymentsRepository
import org.thoughtcrime.securesms.ryan.components.settings.app.subscription.RecurringInAppPaymentRepository
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.InAppPaymentSubscriberRecord
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.subscription.LevelUpdate
import org.thoughtcrime.securesms.ryan.util.InternetConnectionObserver
import org.thoughtcrime.securesms.ryan.util.livedata.Store
import org.whispersystems.signalservice.api.subscriptions.ActiveSubscription
import java.util.Optional

class ManageDonationsViewModel : ViewModel() {

  private val store = Store(ManageDonationsState())
  private val disposables = CompositeDisposable()
  private val networkDisposable: Disposable

  val state: LiveData<ManageDonationsState> = store.stateLiveData
  private val internalDisplayThanksBottomSheetPulse = MutableSharedFlow<Badge>()

  val displayThanksBottomSheetPulse: SharedFlow<Badge> = internalDisplayThanksBottomSheetPulse

  init {
    store.update(Recipient.self().live().liveDataResolved) { self, state ->
      state.copy(featuredBadge = self.featuredBadge)
    }

    networkDisposable = InternetConnectionObserver
      .observe()
      .distinctUntilChanged()
      .subscribe { isConnected ->
        if (isConnected) {
          retry()
        }
      }

    viewModelScope.launch {
      ManageDonationsRepository.consumeSuccessfulIdealPayments()
        .collectLatest {
          internalDisplayThanksBottomSheetPulse.emit(Badges.fromDatabaseBadge(it.data.badge!!))
        }
    }
  }

  override fun onCleared() {
    disposables.clear()
  }

  fun retry() {
    if (!disposables.isDisposed && store.state.subscriptionTransactionState == ManageDonationsState.TransactionState.NetworkFailure) {
      store.update { it.copy(subscriptionTransactionState = ManageDonationsState.TransactionState.Init) }
      refresh()
    }
  }

  fun refresh() {
    disposables.clear()

    val levelUpdateOperationEdges: Observable<Boolean> = LevelUpdate.isProcessing.distinctUntilChanged()
    val activeSubscription: Single<ActiveSubscription> = RecurringInAppPaymentRepository.getActiveSubscription(InAppPaymentSubscriberRecord.Type.DONATION)

    disposables += Single.fromCallable {
      InAppPaymentsRepository.getShouldCancelSubscriptionBeforeNextSubscribeAttempt(InAppPaymentSubscriberRecord.Type.DONATION)
    }.subscribeOn(Schedulers.io()).subscribeBy { requiresCancel ->
      store.update {
        it.copy(subscriberRequiresCancel = requiresCancel)
      }
    }

    disposables += Recipient.observable(Recipient.self().id).map { it.badges }.subscribeBy { badges ->
      store.update { state ->
        state.copy(
          hasOneTimeBadge = badges.any { it.isBoost() }
        )
      }
    }

    disposables += Single.fromCallable { SignalDatabase.donationReceipts.hasReceipts() }.subscribeOn(Schedulers.io()).subscribe { hasReceipts ->
      store.update { it.copy(hasReceipts = hasReceipts) }
    }

    disposables += InAppPaymentsRepository.observeInAppPaymentRedemption(InAppPaymentType.RECURRING_DONATION).subscribeBy { redemptionStatus ->
      store.update { manageDonationsState ->
        manageDonationsState.copy(
          nonVerifiedMonthlyDonation = if (redemptionStatus is DonationRedemptionJobStatus.PendingExternalVerification) redemptionStatus.nonVerifiedMonthlyDonation else null,
          subscriptionRedemptionState = mapStatusToRedemptionState(redemptionStatus)
        )
      }
    }

    disposables += Observable.combineLatest(
      SignalStore.inAppPayments.observablePendingOneTimeDonation,
      InAppPaymentsRepository.observeInAppPaymentRedemption(InAppPaymentType.ONE_TIME_DONATION)
    ) { pendingFromStore, pendingFromJob ->
      if (pendingFromStore.isPresent) {
        pendingFromStore
      } else if (pendingFromJob is DonationRedemptionJobStatus.PendingExternalVerification) {
        Optional.ofNullable(pendingFromJob.pendingOneTimeDonation)
      } else {
        Optional.empty()
      }
    }
      .distinctUntilChanged()
      .subscribeBy { pending ->
        store.update { it.copy(pendingOneTimeDonation = pending.orNull()) }
      }

    disposables += levelUpdateOperationEdges.switchMapSingle { isProcessing ->
      if (isProcessing) {
        Single.just(ManageDonationsState.TransactionState.InTransaction)
      } else {
        activeSubscription.map { ManageDonationsState.TransactionState.NotInTransaction(it) }
      }
    }.subscribeBy(
      onNext = { transactionState ->
        store.update {
          it.copy(subscriptionTransactionState = transactionState)
        }
      },
      onError = { throwable ->
        Log.w(TAG, "Error retrieving subscription transaction state", throwable)

        store.update {
          it.copy(subscriptionTransactionState = ManageDonationsState.TransactionState.NetworkFailure)
        }
      }
    )

    disposables += RecurringInAppPaymentRepository.getSubscriptions().subscribeBy(
      onSuccess = { subs ->
        store.update { it.copy(availableSubscriptions = subs) }
      },
      onError = {
        Log.w(TAG, "Error retrieving subscriptions data", it)
      }
    )
  }

  private fun mapStatusToRedemptionState(status: DonationRedemptionJobStatus): ManageDonationsState.RedemptionState {
    return when (status) {
      DonationRedemptionJobStatus.FailedSubscription -> ManageDonationsState.RedemptionState.FAILED
      DonationRedemptionJobStatus.None -> ManageDonationsState.RedemptionState.NONE
      DonationRedemptionJobStatus.PendingKeepAlive -> ManageDonationsState.RedemptionState.SUBSCRIPTION_REFRESH

      is DonationRedemptionJobStatus.PendingExternalVerification,
      DonationRedemptionJobStatus.PendingReceiptRedemption,
      DonationRedemptionJobStatus.PendingReceiptRequest -> ManageDonationsState.RedemptionState.IN_PROGRESS
    }
  }

  companion object {
    private val TAG = Log.tag(ManageDonationsViewModel::class.java)
  }
}
