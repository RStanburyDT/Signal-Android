package org.thoughtcrime.securesms.ryan.keyboard.emoji

import org.thoughtcrime.securesms.ryan.components.emoji.EmojiPageModel
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

class EmojiPageMappingModel(val key: String, val emojiPageModel: EmojiPageModel) : MappingModel<EmojiPageMappingModel> {
  override fun areItemsTheSame(newItem: EmojiPageMappingModel): Boolean {
    return key == newItem.key
  }

  override fun areContentsTheSame(newItem: EmojiPageMappingModel): Boolean {
    return areItemsTheSame(newItem) &&
      newItem.emojiPageModel.spriteUri == emojiPageModel.spriteUri &&
      newItem.emojiPageModel.iconAttr == emojiPageModel.iconAttr
  }
}
