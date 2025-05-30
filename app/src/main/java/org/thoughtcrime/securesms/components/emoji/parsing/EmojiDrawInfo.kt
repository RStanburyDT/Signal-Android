package org.thoughtcrime.securesms.ryan.components.emoji.parsing

import org.thoughtcrime.securesms.ryan.emoji.EmojiPage

data class EmojiDrawInfo(val page: EmojiPage, val index: Int, val emoji: String, val rawEmoji: String?, val jumboSheet: String?)
