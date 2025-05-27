package org.thoughtcrime.securesms.ryan.reactions.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.signal.core.util.concurrent.SignalExecutors
import org.thoughtcrime.securesms.ryan.components.emoji.EmojiUtil
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.keyvalue.EmojiValues
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.storage.StorageSyncHelper
import org.thoughtcrime.securesms.ryan.util.livedata.LiveDataUtil
import org.thoughtcrime.securesms.ryan.util.livedata.Store

class EditReactionsViewModel : ViewModel() {

  private val emojiValues: EmojiValues = SignalStore.emoji
  private val store: Store<State> = Store(State(reactions = emojiValues.reactions.map { emojiValues.getPreferredVariation(it) }))

  val reactions: LiveData<List<String>> = LiveDataUtil.mapDistinct(store.stateLiveData, State::reactions)
  val selection: LiveData<Int> = LiveDataUtil.mapDistinct(store.stateLiveData, State::selection)

  fun setSelection(selection: Int) {
    store.update { it.copy(selection = selection) }
  }

  fun onEmojiSelected(emoji: String) {
    store.update { state ->
      if (state.selection != NO_SELECTION && state.selection in state.reactions.indices) {
        emojiValues.setPreferredVariation(emoji)
        val preferredEmoji: String = emojiValues.getPreferredVariation(emoji)
        val newReactions: List<String> = state.reactions.toMutableList().apply { set(state.selection, preferredEmoji) }
        state.copy(reactions = newReactions)
      } else {
        state
      }
    }
  }

  fun resetToDefaults() {
    EmojiValues.DEFAULT_REACTIONS_LIST.forEach { emoji ->
      emojiValues.removePreferredVariation(EmojiUtil.getCanonicalRepresentation(emoji))
    }

    store.update { it.copy(reactions = EmojiValues.DEFAULT_REACTIONS_LIST) }
  }

  fun save() {
    emojiValues.reactions = store.state.reactions

    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }
  }

  companion object {
    const val NO_SELECTION: Int = -1
  }

  data class State(val selection: Int = NO_SELECTION, val reactions: List<String>)
}
