package org.thoughtcrime.securesms.ryan.stories.viewer

data class StoryLoadState(
  val isContentReady: Boolean = false,
  val isCrossfaderReady: Boolean = false
) {
  fun isReady(): Boolean = isContentReady && isCrossfaderReady
}
