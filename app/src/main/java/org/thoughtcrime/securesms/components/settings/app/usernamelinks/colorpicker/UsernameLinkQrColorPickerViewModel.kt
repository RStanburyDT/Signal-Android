package org.thoughtcrime.securesms.ryan.components.settings.app.usernamelinks.colorpicker

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.collections.immutable.toImmutableList
import org.signal.core.util.concurrent.SignalExecutors
import org.thoughtcrime.securesms.ryan.components.settings.app.usernamelinks.QrCodeData
import org.thoughtcrime.securesms.ryan.components.settings.app.usernamelinks.QrCodeState
import org.thoughtcrime.securesms.ryan.components.settings.app.usernamelinks.UsernameQrCodeColorScheme
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.profiles.manage.UsernameRepository.toLink
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.storage.StorageSyncHelper

class UsernameLinkQrColorPickerViewModel : ViewModel() {

  private val _state = mutableStateOf(
    UsernameLinkQrColorPickerState(
      username = SignalStore.account.username!!,
      qrCodeData = QrCodeState.Loading,
      colorSchemes = UsernameQrCodeColorScheme.entries.toImmutableList(),
      selectedColorScheme = SignalStore.misc.usernameQrCodeColorScheme
    )
  )

  val state: State<UsernameLinkQrColorPickerState> = _state

  private val disposable: CompositeDisposable = CompositeDisposable()

  init {
    val usernameLink = SignalStore.account.usernameLink

    if (usernameLink != null) {
      disposable += Single
        .fromCallable { QrCodeData.forData(usernameLink.toLink()) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { qrData ->
          _state.value = _state.value.copy(
            qrCodeData = QrCodeState.Present(qrData)
          )
        }
    } else {
      _state.value = _state.value.copy(
        qrCodeData = QrCodeState.NotSet
      )
    }
  }

  override fun onCleared() {
    disposable.clear()
  }

  fun onColorSelected(color: UsernameQrCodeColorScheme) {
    SignalStore.misc.usernameQrCodeColorScheme = color
    SignalExecutors.BOUNDED.run {
      SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }

    _state.value = _state.value.copy(
      selectedColorScheme = color
    )
  }
}
