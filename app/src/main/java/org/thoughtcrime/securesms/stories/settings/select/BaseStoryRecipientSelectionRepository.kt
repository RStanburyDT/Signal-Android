package org.thoughtcrime.securesms.ryan.stories.settings.select

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.CursorUtil
import org.signal.core.util.concurrent.SignalExecutors
import org.thoughtcrime.securesms.ryan.database.RecipientTable
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.DistributionListId
import org.thoughtcrime.securesms.ryan.database.model.DistributionListRecord
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.stories.Stories

class BaseStoryRecipientSelectionRepository {

  fun getRecord(distributionListId: DistributionListId): Single<DistributionListRecord> {
    return Single.fromCallable {
      SignalDatabase.distributionLists.getList(distributionListId) ?: error("Record does not exist.")
    }.subscribeOn(Schedulers.io())
  }

  fun updateDistributionListMembership(distributionListRecord: DistributionListRecord, recipients: Set<RecipientId>) {
    SignalExecutors.BOUNDED.execute {
      val currentRecipients = SignalDatabase.distributionLists.getRawMembers(distributionListRecord.id, distributionListRecord.privacyMode).toSet()
      val oldNotNew = currentRecipients - recipients
      val newNotOld = recipients - currentRecipients

      oldNotNew.forEach {
        SignalDatabase.distributionLists.removeMemberFromList(distributionListRecord.id, distributionListRecord.privacyMode, it)
      }

      newNotOld.forEach {
        SignalDatabase.distributionLists.addMemberToList(distributionListRecord.id, distributionListRecord.privacyMode, it)
      }

      Stories.onStorySettingsChanged(distributionListRecord.id)
    }
  }

  fun getAllSignalContacts(): Single<Set<RecipientId>> {
    return Single.fromCallable {
      SignalDatabase.recipients.getSignalContacts(RecipientTable.IncludeSelfMode.Exclude).use {
        val recipientSet = mutableSetOf<RecipientId>()
        while (it.moveToNext()) {
          recipientSet.add(RecipientId.from(CursorUtil.requireLong(it, RecipientTable.ID)))
        }

        recipientSet
      } ?: emptySet()
    }.subscribeOn(Schedulers.io())
  }
}
