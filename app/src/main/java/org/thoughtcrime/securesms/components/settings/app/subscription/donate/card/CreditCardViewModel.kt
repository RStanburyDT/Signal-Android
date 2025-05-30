package org.thoughtcrime.securesms.ryan.components.settings.app.subscription.donate.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.processors.BehaviorProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.signal.donations.StripeApi
import org.thoughtcrime.securesms.ryan.database.InAppPaymentTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.util.rx.RxStore
import java.util.Calendar

class CreditCardViewModel(
  inAppPaymentId: InAppPaymentTable.InAppPaymentId
) : ViewModel() {

  private val formStore = RxStore(CreditCardFormState())
  private val validationProcessor: BehaviorProcessor<CreditCardValidationState> = BehaviorProcessor.create()
  private val currentYear: Int
  private val currentMonth: Int

  private val internalInAppPayment = MutableStateFlow<InAppPaymentTable.InAppPayment?>(null)
  val inAppPayment: Flow<InAppPaymentTable.InAppPayment> = internalInAppPayment.filterNotNull()

  private val disposables = CompositeDisposable()

  init {
    val calendar = Calendar.getInstance()

    viewModelScope.launch {
      val inAppPayment = withContext(Dispatchers.IO) {
        SignalDatabase.inAppPayments.getById(inAppPaymentId)!!
      }

      internalInAppPayment.update { inAppPayment }
    }

    currentYear = calendar.get(Calendar.YEAR)
    currentMonth = calendar.get(Calendar.MONTH) + 1

    disposables += formStore.stateFlowable.subscribe { formState ->
      val type = CreditCardType.fromCardNumber(formState.number)
      validationProcessor.onNext(
        CreditCardValidationState(
          type = type,
          numberValidity = CreditCardNumberValidator.getValidity(formState.number, formState.focusedField == CreditCardFormState.FocusedField.NUMBER),
          expirationValidity = CreditCardExpirationValidator.getValidity(formState.expiration, currentMonth, currentYear, formState.focusedField == CreditCardFormState.FocusedField.EXPIRATION),
          codeValidity = CreditCardCodeValidator.getValidity(formState.code, type, formState.focusedField == CreditCardFormState.FocusedField.CODE)
        )
      )
    }
  }

  val state: Flowable<CreditCardValidationState> = validationProcessor.distinctUntilChanged().observeOn(AndroidSchedulers.mainThread())
  val currentFocusField: CreditCardFormState.FocusedField
    get() = formStore.state.focusedField

  override fun onCleared() {
    disposables.clear()
    formStore.dispose()
  }

  fun onNumberChanged(number: String) {
    formStore.update {
      it.copy(number = number)
    }
  }

  fun onNumberFocusChanged(isFocused: Boolean) {
    updateFocus(CreditCardFormState.FocusedField.NUMBER, isFocused)
  }

  fun onExpirationChanged(expiration: String) {
    formStore.update {
      it.copy(expiration = CreditCardExpiration.fromInput(expiration))
    }
  }

  fun onExpirationFocusChanged(isFocused: Boolean) {
    updateFocus(CreditCardFormState.FocusedField.EXPIRATION, isFocused)
  }

  fun onCodeChanged(code: String) {
    formStore.update {
      it.copy(code = code)
    }
  }

  fun onCodeFocusChanged(isFocused: Boolean) {
    updateFocus(CreditCardFormState.FocusedField.CODE, isFocused)
  }

  fun getCardData(): StripeApi.CardData {
    return formStore.state.toCardData()
  }

  private fun updateFocus(
    newFocusedField: CreditCardFormState.FocusedField,
    isFocused: Boolean
  ) {
    formStore.update {
      it.copy(focusedField = getUpdatedFocus(it.focusedField, newFocusedField, isFocused))
    }
  }

  private fun getUpdatedFocus(
    currentFocusedField: CreditCardFormState.FocusedField,
    newFocusedField: CreditCardFormState.FocusedField,
    isFocused: Boolean
  ): CreditCardFormState.FocusedField {
    return if (currentFocusedField == newFocusedField && !isFocused) {
      CreditCardFormState.FocusedField.NONE
    } else if (isFocused) {
      newFocusedField
    } else {
      currentFocusedField
    }
  }
}
