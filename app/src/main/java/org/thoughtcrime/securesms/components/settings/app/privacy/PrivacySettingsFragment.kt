package org.thoughtcrime.securesms.ryan.components.settings.app.privacy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.BiometricDeviceAuthentication
import org.thoughtcrime.securesms.ryan.BiometricDeviceLockContract
import org.thoughtcrime.securesms.ryan.PassphraseChangeActivity
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.TimeDurationPickerDialog
import org.thoughtcrime.securesms.ryan.components.settings.ClickPreference
import org.thoughtcrime.securesms.ryan.components.settings.ClickPreferenceViewHolder
import org.thoughtcrime.securesms.ryan.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.ryan.components.settings.PreferenceModel
import org.thoughtcrime.securesms.ryan.components.settings.PreferenceViewHolder
import org.thoughtcrime.securesms.ryan.components.settings.configure
import org.thoughtcrime.securesms.ryan.crypto.MasterSecretUtil
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.service.KeyCachingService
import org.thoughtcrime.securesms.ryan.util.CommunicationActions
import org.thoughtcrime.securesms.ryan.util.ExpirationUtil
import org.thoughtcrime.securesms.ryan.util.ServiceUtil
import org.thoughtcrime.securesms.ryan.util.SpanUtil
import org.thoughtcrime.securesms.ryan.util.TextSecurePreferences
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.ryan.util.navigation.safeNavigate
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TAG = Log.tag(PrivacySettingsFragment::class.java)

class PrivacySettingsFragment : DSLSettingsFragment(R.string.preferences__privacy) {

  private lateinit var viewModel: PrivacySettingsViewModel
  private lateinit var biometricAuth: BiometricDeviceAuthentication
  private lateinit var biometricDeviceLockLauncher: ActivityResultLauncher<String>

  private val incognitoSummary: CharSequence by lazy {
    SpannableStringBuilder(getString(R.string.preferences__this_setting_is_not_a_guarantee))
      .append(" ")
      .append(
        SpanUtil.learnMore(requireContext(), ContextCompat.getColor(requireContext(), R.color.signal_text_primary)) {
          CommunicationActions.openBrowserLink(requireContext(), getString(R.string.preferences__incognito_keyboard_learn_more))
        }
      )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    biometricDeviceLockLauncher = registerForActivityResult(BiometricDeviceLockContract()) { result: Int ->
      if (result == BiometricDeviceAuthentication.AUTHENTICATED) {
        viewModel.togglePaymentLock(false)
      }
    }
    val promptInfo = PromptInfo.Builder()
      .setAllowedAuthenticators(BiometricDeviceAuthentication.ALLOWED_AUTHENTICATORS)
      .setTitle(requireContext().getString(R.string.BiometricDeviceAuthentication__signal))
      .setConfirmationRequired(false)
      .build()
    biometricAuth = BiometricDeviceAuthentication(
      BiometricManager.from(requireActivity()),
      BiometricPrompt(requireActivity(), BiometricAuthenticationListener()),
      promptInfo
    )
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshBlockedCount()
  }

  override fun onPause() {
    super.onPause()
    biometricAuth.cancelAuthentication()
  }

  override fun bindAdapter(adapter: MappingAdapter) {
    adapter.registerFactory(ValueClickPreference::class.java, LayoutFactory(::ValueClickPreferenceViewHolder, R.layout.value_click_preference_item))

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    val repository = PrivacySettingsRepository()
    val factory = PrivacySettingsViewModel.Factory(sharedPreferences, repository)
    viewModel = ViewModelProvider(this, factory)[PrivacySettingsViewModel::class.java]
    val args: PrivacySettingsFragmentArgs by navArgs()
    var showPaymentLock = true

    viewModel.state.observe(viewLifecycleOwner) { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
      if (args.showPaymentLock && showPaymentLock) {
        showPaymentLock = false
        recyclerView?.scrollToPosition(adapter.itemCount - 1)
      }
    }
  }

  private fun getConfiguration(state: PrivacySettingsState): DSLConfiguration {
    return configure {
      clickPref(
        title = DSLSettingsText.from(R.string.preferences_app_protection__phone_number),
        summary = DSLSettingsText.from(R.string.preferences_app_protection__choose_who_can_see),
        onClick = {
          Navigation.findNavController(requireView())
            .safeNavigate(R.id.action_privacySettingsFragment_to_phoneNumberPrivacySettingsFragment)
        }
      )

      dividerPref()

      clickPref(
        title = DSLSettingsText.from(R.string.PrivacySettingsFragment__blocked),
        summary = DSLSettingsText.from(resources.getQuantityString(R.plurals.PrivacySettingsFragment__d_contacts, state.blockedCount, state.blockedCount)),
        onClick = {
          Navigation.findNavController(requireView())
            .safeNavigate(R.id.action_privacySettingsFragment_to_blockedUsersActivity)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.PrivacySettingsFragment__messaging)

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__read_receipts),
        summary = DSLSettingsText.from(R.string.preferences__if_read_receipts_are_disabled_you_wont_be_able_to_see_read_receipts),
        isChecked = state.readReceipts,
        onClick = {
          viewModel.setReadReceiptsEnabled(!state.readReceipts)
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__typing_indicators),
        summary = DSLSettingsText.from(R.string.preferences__if_typing_indicators_are_disabled_you_wont_be_able_to_see_typing_indicators),
        isChecked = state.typingIndicators,
        onClick = {
          viewModel.setTypingIndicatorsEnabled(!state.typingIndicators)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.PrivacySettingsFragment__disappearing_messages)

      customPref(
        ValueClickPreference(
          value = DSLSettingsText.from(ExpirationUtil.getExpirationAbbreviatedDisplayValue(requireContext(), state.universalExpireTimer)),
          clickPreference = ClickPreference(
            title = DSLSettingsText.from(R.string.PrivacySettingsFragment__default_timer_for_new_changes),
            summary = DSLSettingsText.from(R.string.PrivacySettingsFragment__set_a_default_disappearing_message_timer_for_all_new_chats_started_by_you),
            onClick = {
              NavHostFragment.findNavController(this@PrivacySettingsFragment).safeNavigate(R.id.action_privacySettingsFragment_to_disappearingMessagesTimerSelectFragment)
            }
          )
        )
      )

      dividerPref()

      sectionHeaderPref(R.string.PrivacySettingsFragment__app_security)

      if (state.isObsoletePasswordEnabled) {
        switchPref(
          title = DSLSettingsText.from(R.string.preferences__enable_passphrase),
          summary = DSLSettingsText.from(R.string.preferences__lock_signal_and_message_notifications_with_a_passphrase),
          isChecked = true,
          onClick = {
            MaterialAlertDialogBuilder(requireContext()).apply {
              setTitle(R.string.ApplicationPreferencesActivity_disable_passphrase)
              setMessage(R.string.ApplicationPreferencesActivity_this_will_permanently_unlock_signal_and_message_notifications)
              setIcon(R.drawable.symbol_error_triangle_fill_24)
              setPositiveButton(R.string.ApplicationPreferencesActivity_disable) { _, _ ->
                MasterSecretUtil.changeMasterSecretPassphrase(
                  activity,
                  KeyCachingService.getMasterSecret(context),
                  MasterSecretUtil.UNENCRYPTED_PASSPHRASE
                )
                SignalStore.settings.passphraseDisabled = true
                val intent = Intent(activity, KeyCachingService::class.java)
                intent.action = KeyCachingService.DISABLE_ACTION
                requireActivity().startService(intent)
                viewModel.refresh()
              }
              setNegativeButton(android.R.string.cancel, null)
              show()
            }
          }
        )

        clickPref(
          title = DSLSettingsText.from(R.string.preferences__change_passphrase),
          summary = DSLSettingsText.from(R.string.preferences__change_your_passphrase),
          onClick = {
            if (MasterSecretUtil.isPassphraseInitialized(activity)) {
              startActivity(Intent(activity, PassphraseChangeActivity::class.java))
            } else {
              Toast.makeText(
                activity,
                R.string.ApplicationPreferenceActivity_you_havent_set_a_passphrase_yet,
                Toast.LENGTH_LONG
              ).show()
            }
          }
        )

        switchPref(
          title = DSLSettingsText.from(R.string.preferences__inactivity_timeout_passphrase),
          summary = DSLSettingsText.from(R.string.preferences__auto_lock_signal_after_a_specified_time_interval_of_inactivity),
          isChecked = state.isObsoletePasswordTimeoutEnabled,
          onClick = {
            viewModel.setObsoletePasswordTimeoutEnabled(!state.isObsoletePasswordTimeoutEnabled)
          }
        )

        clickPref(
          title = DSLSettingsText.from(R.string.preferences__inactivity_timeout_interval),
          onClick = {
            childFragmentManager.clearFragmentResult(TimeDurationPickerDialog.RESULT_DURATION)
            childFragmentManager.clearFragmentResultListener(TimeDurationPickerDialog.RESULT_DURATION)
            childFragmentManager.setFragmentResultListener(TimeDurationPickerDialog.RESULT_DURATION, this@PrivacySettingsFragment) { _, bundle ->
              val timeout = bundle.getLong(TimeDurationPickerDialog.RESULT_KEY_DURATION_MILLISECONDS).milliseconds.inWholeMinutes.toInt()
              viewModel.setObsoletePasswordTimeout(max(timeout, 1))
            }
            TimeDurationPickerDialog.create(state.screenLockActivityTimeout.seconds).show(childFragmentManager, null)
          }
        )
      } else {
        val isKeyguardSecure = ServiceUtil.getKeyguardManager(requireContext()).isKeyguardSecure

        clickPref(
          title = DSLSettingsText.from(R.string.preferences_app_protection__screen_lock),
          summary = DSLSettingsText.from(getScreenLockInactivityTimeoutSummary(isKeyguardSecure && state.screenLock, state.screenLockActivityTimeout)),
          onClick = {
            Navigation.findNavController(requireView()).safeNavigate(R.id.action_privacySettingsFragment_to_screenLockSettingsFragment)
          },
          isEnabled = isKeyguardSecure,
          onDisabledClicked = {
            Snackbar
              .make(
                requireView(),
                resources.getString(R.string.preferences_app_protection__to_use_screen_lock),
                Snackbar.LENGTH_LONG
              )
              .show()
          }
        )
      }

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__screen_security),
        summary = DSLSettingsText.from(R.string.PrivacySettingsFragment__block_screenshots_in_the_recents_list_and_inside_the_app),
        isChecked = state.screenSecurity,
        onClick = {
          viewModel.setScreenSecurityEnabled(!state.screenSecurity)

          if (TextSecurePreferences.isScreenSecurityEnabled(requireContext())) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
          } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
          }
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__incognito_keyboard),
        summary = DSLSettingsText.from(R.string.preferences__request_keyboard_to_disable),
        isChecked = state.incognitoKeyboard,
        onClick = {
          viewModel.setIncognitoKeyboard(!state.incognitoKeyboard)
        }
      )

      textPref(
        summary = DSLSettingsText.from(incognitoSummary)
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_app_protection__payments)

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__payment_lock),
        summary = DSLSettingsText.from(R.string.PrivacySettingsFragment__payment_lock_require_lock),
        isChecked = state.paymentLock && ServiceUtil.getKeyguardManager(requireContext()).isKeyguardSecure,
        onClick = {
          if (!ServiceUtil.getKeyguardManager(requireContext()).isKeyguardSecure) {
            showGoToPhoneSettings()
          } else if (state.paymentLock) {
            biometricAuth.authenticate(requireContext(), true) { biometricDeviceLockLauncher.launch(getString(R.string.BiometricDeviceAuthentication__signal)) }
          } else {
            viewModel.togglePaymentLock(true)
          }
        }
      )

      dividerPref()

      clickPref(
        title = DSLSettingsText.from(R.string.preferences__advanced),
        summary = DSLSettingsText.from(R.string.PrivacySettingsFragment__signal_message_and_calls),
        onClick = {
          Navigation.findNavController(requireView()).safeNavigate(R.id.action_privacySettingsFragment_to_advancedPrivacySettingsFragment)
        }
      )
    }
  }

  private fun showGoToPhoneSettings() {
    MaterialAlertDialogBuilder(requireContext()).apply {
      setTitle(getString(R.string.PrivacySettingsFragment__cant_enable_title))
      setMessage(getString(R.string.PrivacySettingsFragment__cant_enable_description))
      setPositiveButton(R.string.PaymentsHomeFragment__enable) { _, _ ->
        val intent = when {
          Build.VERSION.SDK_INT >= 30 -> Intent(Settings.ACTION_BIOMETRIC_ENROLL)
          Build.VERSION.SDK_INT >= 28 -> Intent(Settings.ACTION_FINGERPRINT_ENROLL)
          else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        try {
          startActivity(intent)
        } catch (e: ActivityNotFoundException) {
          Log.w(TAG, "Failed to navigate to system settings.", e)
          Toast.makeText(requireContext(), R.string.PrivacySettingsFragment__failed_to_navigate_to_system_settings, Toast.LENGTH_SHORT).show()
        }
      }
      setNegativeButton(R.string.PaymentsHomeFragment__not_now) { _, _ -> }
      show()
    }
  }

  private fun getScreenLockInactivityTimeoutSummary(enabledScreenLock: Boolean, timeoutSeconds: Long): String {
    return if (!enabledScreenLock) {
      getString(R.string.ScreenLockSettingsFragment__off)
    } else if (timeoutSeconds == 0L) {
      Log.i(TAG, "Default immediate screen lock to one minute")
      ExpirationUtil.getExpirationDisplayValue(requireContext(), 60)
    } else {
      ExpirationUtil.getExpirationDisplayValue(requireContext(), timeoutSeconds.toInt())
    }
  }

  private class ValueClickPreference(
    val value: DSLSettingsText,
    val clickPreference: ClickPreference
  ) : PreferenceModel<ValueClickPreference>(
    title = clickPreference.title,
    summary = clickPreference.summary,
    icon = clickPreference.icon,
    isEnabled = clickPreference.isEnabled
  ) {
    override fun areContentsTheSame(newItem: ValueClickPreference): Boolean {
      return super.areContentsTheSame(newItem) &&
        clickPreference == newItem.clickPreference &&
        value == newItem.value
    }
  }

  private class ValueClickPreferenceViewHolder(itemView: View) : PreferenceViewHolder<ValueClickPreference>(itemView) {
    private val clickPreferenceViewHolder = ClickPreferenceViewHolder(itemView)
    private val valueText: TextView = findViewById(R.id.value_client_preference_value)

    override fun bind(model: ValueClickPreference) {
      super.bind(model)
      clickPreferenceViewHolder.bind(model.clickPreference)
      valueText.text = model.value.resolve(context)
    }
  }

  inner class BiometricAuthenticationListener : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(errorCode: Int, errorString: CharSequence) {
      Log.w(TAG, "Authentication error: $errorCode")
      onAuthenticationFailed()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
      Log.i(TAG, "onAuthenticationSucceeded")
      viewModel.togglePaymentLock(false)
    }

    override fun onAuthenticationFailed() {
      Log.w(TAG, "Unable to authenticate payment lock")
      viewModel.refresh()
    }
  }
}
