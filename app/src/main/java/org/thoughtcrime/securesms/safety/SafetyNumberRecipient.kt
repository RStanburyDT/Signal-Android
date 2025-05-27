package org.thoughtcrime.securesms.ryan.safety

import org.thoughtcrime.securesms.ryan.database.model.IdentityRecord
import org.thoughtcrime.securesms.ryan.recipients.Recipient

/**
 * Represents a Recipient who had a safety number change. Also includes information used in
 * order to determine whether the recipient should be shown on a given screen and what menu
 * options it should have.
 */
data class SafetyNumberRecipient(
  val recipient: Recipient,
  val identityRecord: IdentityRecord,
  val distributionListMembershipCount: Int,
  val groupMembershipCount: Int
)
