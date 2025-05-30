package org.thoughtcrime.securesms.ryan.stories.settings.create

import org.thoughtcrime.securesms.ryan.recipients.RecipientId

data class CreateStoryWithViewersState(
  val label: CharSequence = "",
  val error: NameError? = null,
  val saveState: SaveState = SaveState.Init
) {
  enum class NameError {
    NO_LABEL,
    DUPLICATE_LABEL
  }

  sealed class SaveState {
    object Init : SaveState()
    object Saving : SaveState()
    data class Saved(val recipientId: RecipientId) : SaveState()
  }
}
