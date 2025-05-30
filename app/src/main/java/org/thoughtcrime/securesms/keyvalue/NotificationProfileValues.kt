package org.thoughtcrime.securesms.ryan.keyvalue

/**
 * Values for managing enable/disable state and corresponding alerts for Notification Profiles.
 */
class NotificationProfileValues(store: KeyValueStore) : SignalStoreValues(store) {

  companion object {
    private const val KEY_LAST_PROFILE_POPUP = "np.last_profile_popup"
    private const val KEY_LAST_PROFILE_POPUP_TIME = "np.last_profile_popup_time"
    private const val KEY_SEEN_TOOLTIP = "np.seen_tooltip"

    private const val KEY_MANUALLY_ENABLED_PROFILE = "np.manually_enabled_profile"
    private const val KEY_MANUALLY_ENABLED_UNTIL = "np.manually_enabled_until"
    private const val KEY_MANUALLY_DISABLED_AT = "np.manually_disabled_at"
  }

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): MutableList<String> {
    return mutableListOf(KEY_SEEN_TOOLTIP)
  }

  var manuallyEnabledProfile: Long by longValue(KEY_MANUALLY_ENABLED_PROFILE, 0L)
  var manuallyEnabledUntil: Long by longValue(KEY_MANUALLY_ENABLED_UNTIL, 0L)
  var manuallyDisabledAt: Long by longValue(KEY_MANUALLY_DISABLED_AT, 0L)

  var lastProfilePopup: Long by longValue(KEY_LAST_PROFILE_POPUP, 0L)
  var lastProfilePopupTime: Long by longValue(KEY_LAST_PROFILE_POPUP_TIME, 0L)
  var hasSeenTooltip: Boolean by booleanValue(KEY_SEEN_TOOLTIP, false)
}
