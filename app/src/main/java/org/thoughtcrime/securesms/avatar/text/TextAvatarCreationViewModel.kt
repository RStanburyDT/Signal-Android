package org.thoughtcrime.securesms.ryan.avatar.text

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import org.thoughtcrime.securesms.ryan.avatar.Avatar
import org.thoughtcrime.securesms.ryan.avatar.Avatars
import org.thoughtcrime.securesms.ryan.util.livedata.Store

class TextAvatarCreationViewModel(initialText: Avatar.Text) : ViewModel() {

  private val store = Store(TextAvatarCreationState(initialText))

  val state: LiveData<TextAvatarCreationState> = store.stateLiveData.distinctUntilChanged()

  fun setColor(colorPair: Avatars.ColorPair) {
    store.update { it.copy(currentAvatar = it.currentAvatar.copy(color = colorPair)) }
  }

  fun setText(text: String) {
    store.update {
      if (it.currentAvatar.text == text) {
        it
      } else {
        it.copy(currentAvatar = it.currentAvatar.copy(text = text))
      }
    }
  }

  fun getCurrentAvatar(): Avatar.Text {
    return store.state.currentAvatar
  }

  class Factory(private val initialText: Avatar.Text) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(TextAvatarCreationViewModel(initialText)))
    }
  }
}
