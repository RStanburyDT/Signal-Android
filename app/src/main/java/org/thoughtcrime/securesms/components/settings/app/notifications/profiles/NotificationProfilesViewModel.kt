package org.thoughtcrime.securesms.ryan.components.settings.app.notifications.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import org.thoughtcrime.securesms.ryan.notifications.profiles.NotificationProfile

class NotificationProfilesViewModel(private val repository: NotificationProfilesRepository) : ViewModel() {

  fun getProfiles(): Flowable<List<NotificationProfile>> {
    return repository.getProfiles()
      .observeOn(AndroidSchedulers.mainThread())
  }

  class Factory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(NotificationProfilesViewModel(NotificationProfilesRepository()))!!
    }
  }
}
