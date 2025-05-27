package org.thoughtcrime.securesms.ryan.keyboard.emoji

import org.thoughtcrime.securesms.ryan.components.emoji.EmojiPageModel
import org.thoughtcrime.securesms.ryan.components.emoji.EmojiPageViewGridAdapter
import org.thoughtcrime.securesms.ryan.components.emoji.RecentEmojiPageModel
import org.thoughtcrime.securesms.ryan.components.emoji.parsing.EmojiTree
import org.thoughtcrime.securesms.ryan.emoji.EmojiCategory
import org.thoughtcrime.securesms.ryan.emoji.EmojiSource
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

fun EmojiPageModel.toMappingModels(): List<MappingModel<*>> {
  val emojiTree: EmojiTree = EmojiSource.latest.emojiTree

  return displayEmoji.map {
    val isTextEmoji = EmojiCategory.EMOTICONS.key == key || (RecentEmojiPageModel.KEY == key && emojiTree.getEmoji(it.value, 0, it.value.length) == null)

    if (isTextEmoji) {
      EmojiPageViewGridAdapter.EmojiTextModel(key, it)
    } else {
      EmojiPageViewGridAdapter.EmojiModel(key, it)
    }
  }
}
