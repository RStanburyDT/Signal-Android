package org.thoughtcrime.securesms.ryan.components.settings.app.notifications.manual

import org.thoughtcrime.securesms.ryan.notifications.profiles.NotificationProfile
import java.time.LocalDateTime

data class NotificationProfileSelectionState(
  val notificationProfiles: List<NotificationProfile> = listOf(),
  val expandedId: Long = -1L,
  val timeSlotB: LocalDateTime
)
