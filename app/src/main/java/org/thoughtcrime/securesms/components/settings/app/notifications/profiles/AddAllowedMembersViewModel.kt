package org.thoughtcrime.securesms.ryan.components.settings.app.notifications.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.thoughtcrime.securesms.ryan.notifications.profiles.NotificationProfile
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.recipients.RecipientId

class AddAllowedMembersViewModel(private val profileId: Long, private val repository: NotificationProfilesRepository) : ViewModel() {

  fun getProfile(): Observable<NotificationProfileAndRecipients> {
    return repository.getProfile(profileId)
      .map { profile ->
        NotificationProfileAndRecipients(profile, profile.allowedMembers.map { Recipient.resolved(it) })
      }
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun addMember(id: RecipientId): Single<NotificationProfile> {
    return repository.addMember(profileId, id)
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun removeMember(id: RecipientId): Single<Recipient> {
    return repository.removeMember(profileId, id)
      .map { Recipient.resolved(id) }
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun toggleAllowAllMentions(): Single<NotificationProfile> {
    return repository.toggleAllowAllMentions(profileId)
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun toggleAllowAllCalls(): Single<NotificationProfile> {
    return repository.toggleAllowAllCalls(profileId)
      .observeOn(AndroidSchedulers.mainThread())
  }

  data class NotificationProfileAndRecipients(val profile: NotificationProfile, val recipients: List<Recipient>)

  class Factory(private val profileId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(AddAllowedMembersViewModel(profileId, NotificationProfilesRepository()))!!
    }
  }
}
