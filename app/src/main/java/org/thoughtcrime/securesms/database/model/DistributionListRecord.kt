package org.thoughtcrime.securesms.ryan.database.model

import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.whispersystems.signalservice.api.push.DistributionId

/**
 * Represents an entry in the [org.thoughtcrime.securesms.ryan.database.DistributionListTables].
 */
data class DistributionListRecord(
  val id: DistributionListId,
  val name: String,
  val distributionId: DistributionId,
  val allowsReplies: Boolean,
  val rawMembers: List<RecipientId>,
  val members: List<RecipientId>,
  val deletedAtTimestamp: Long,
  val isUnknown: Boolean,
  val privacyMode: DistributionListPrivacyMode
) {
  fun getMembersToSync(): List<RecipientId> {
    return when (privacyMode) {
      DistributionListPrivacyMode.ALL -> emptyList()
      else -> rawMembers
    }
  }
}
