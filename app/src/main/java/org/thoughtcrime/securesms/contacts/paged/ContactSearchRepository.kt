package org.thoughtcrime.securesms.ryan.contacts.paged

import androidx.annotation.CheckResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.thoughtcrime.securesms.ryan.database.GroupTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.DistributionListId
import org.thoughtcrime.securesms.ryan.groups.GroupId
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.storage.StorageSyncHelper
import org.thoughtcrime.securesms.ryan.stories.Stories

class ContactSearchRepository {
  @CheckResult
  fun filterOutUnselectableContactSearchKeys(contactSearchKeys: Set<ContactSearchKey>): Single<Set<ContactSearchSelectionResult>> {
    return Single.fromCallable {
      contactSearchKeys.map {
        val isSelectable = when (it) {
          is ContactSearchKey.RecipientSearchKey -> canSelectRecipient(it.recipientId)
          is ContactSearchKey.UnknownRecipientKey -> it.sectionKey == ContactSearchConfiguration.SectionKey.PHONE_NUMBER
          is ContactSearchKey.ChatTypeSearchKey -> true
          else -> false
        }
        ContactSearchSelectionResult(it, isSelectable)
      }.toSet()
    }
  }

  private fun canSelectRecipient(recipientId: RecipientId): Boolean {
    val recipient = Recipient.resolved(recipientId)
    return if (recipient.isPushV2Group) {
      val record = SignalDatabase.groups.getGroup(recipient.requireGroupId())
      !(record.isPresent && record.get().isAnnouncementGroup && !record.get().isAdmin(Recipient.self()))
    } else {
      true
    }
  }

  @CheckResult
  fun markDisplayAsStory(recipientIds: Collection<RecipientId>): Completable {
    return Completable.fromAction {
      SignalDatabase.groups.setShowAsStoryState(recipientIds, GroupTable.ShowAsStoryState.ALWAYS)
      SignalDatabase.recipients.markNeedsSync(recipientIds)
      StorageSyncHelper.scheduleSyncForDataChange()
    }.subscribeOn(Schedulers.io())
  }

  @CheckResult
  fun unmarkDisplayAsStory(groupId: GroupId): Completable {
    return Completable.fromAction {
      SignalDatabase.groups.setShowAsStoryState(groupId, GroupTable.ShowAsStoryState.NEVER)
      SignalDatabase.recipients.markNeedsSync(Recipient.externalGroupExact(groupId).id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }.subscribeOn(Schedulers.io())
  }

  @CheckResult
  fun deletePrivateStory(distributionListId: DistributionListId): Completable {
    return Completable.fromAction {
      SignalDatabase.distributionLists.deleteList(distributionListId)
      Stories.onStorySettingsChanged(distributionListId)
    }.subscribeOn(Schedulers.io())
  }
}
