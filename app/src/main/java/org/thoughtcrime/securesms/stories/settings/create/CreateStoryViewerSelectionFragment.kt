package org.thoughtcrime.securesms.ryan.stories.settings.create

import androidx.navigation.fragment.findNavController
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.database.model.DistributionListId
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.stories.settings.select.BaseStoryRecipientSelectionFragment
import org.thoughtcrime.securesms.ryan.util.navigation.safeNavigate

/**
 * Allows user to select who will see the story they are creating
 */
class CreateStoryViewerSelectionFragment : BaseStoryRecipientSelectionFragment() {
  override val actionButtonLabel: Int = R.string.CreateStoryViewerSelectionFragment__next
  override val distributionListId: DistributionListId? = null

  override fun goToNextScreen(recipients: Set<RecipientId>) {
    findNavController().safeNavigate(CreateStoryViewerSelectionFragmentDirections.actionCreateStoryViewerSelectionToCreateStoryWithViewers(recipients.toTypedArray()))
  }
}
