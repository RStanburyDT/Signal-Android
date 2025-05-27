package org.thoughtcrime.securesms.ryan.components.settings.conversation.permissions

import org.thoughtcrime.securesms.ryan.groups.ui.GroupChangeFailureReason

sealed class PermissionsSettingsEvents {
  class GroupChangeError(val reason: GroupChangeFailureReason) : PermissionsSettingsEvents()
}
