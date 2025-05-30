package org.thoughtcrime.securesms.ryan.mediasend.v2.capture

import org.thoughtcrime.securesms.ryan.mediasend.Media
import org.thoughtcrime.securesms.ryan.recipients.Recipient

sealed interface MediaCaptureEvent {
  data class MediaCaptureRendered(val media: Media) : MediaCaptureEvent
  data class UsernameScannedFromQrCode(val recipient: Recipient, val username: String) : MediaCaptureEvent
  data object DeviceLinkScannedFromQrCode : MediaCaptureEvent
  data object MediaCaptureRenderFailed : MediaCaptureEvent
  data class ReregistrationScannedFromQrCode(val data: String) : MediaCaptureEvent
}
