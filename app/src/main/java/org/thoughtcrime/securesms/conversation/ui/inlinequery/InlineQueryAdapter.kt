package org.thoughtcrime.securesms.ryan.conversation.ui.inlinequery

import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.AnyMappingModel
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter

class InlineQueryAdapter(listener: (AnyMappingModel) -> Unit) : MappingAdapter() {
  init {
    registerFactory(InlineQueryEmojiResult.Model::class.java, { InlineQueryEmojiResult.ViewHolder(it, listener) }, R.layout.inline_query_emoji_result)
  }
}
