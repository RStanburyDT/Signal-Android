package org.thoughtcrime.securesms.ryan.video.interfaces

fun interface TranscoderCancelationSignal {
  fun isCanceled(): Boolean
}
