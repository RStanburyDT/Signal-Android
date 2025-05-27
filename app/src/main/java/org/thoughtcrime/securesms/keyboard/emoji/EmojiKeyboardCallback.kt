package org.thoughtcrime.securesms.ryan.keyboard.emoji

import org.thoughtcrime.securesms.ryan.components.emoji.EmojiEventListener
import org.thoughtcrime.securesms.ryan.keyboard.emoji.search.EmojiSearchFragment

interface EmojiKeyboardCallback :
  EmojiEventListener,
  EmojiKeyboardPageFragment.Callback,
  EmojiSearchFragment.Callback
