package org.thoughtcrime.securesms.ryan.contacts.sync

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.FixedRoundedCornerBottomSheetDialogFragment
import org.thoughtcrime.securesms.ryan.databinding.CdsTemporaryErrorBottomSheetBinding
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.util.BottomSheetUtil
import org.thoughtcrime.securesms.ryan.util.CommunicationActions
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bottom sheet shown when CDS is rate-limited, preventing us from temporarily doing a refresh.
 */
class CdsTemporaryErrorBottomSheet : FixedRoundedCornerBottomSheetDialogFragment() {

  private lateinit var binding: CdsTemporaryErrorBottomSheetBinding

  override val peekHeightPercentage: Float = 0.75f

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    binding = CdsTemporaryErrorBottomSheetBinding.inflate(inflater.cloneInContext(ContextThemeWrapper(inflater.context, themeResId)), container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val days: Int = (SignalStore.misc.cdsBlockedUtil - System.currentTimeMillis()).milliseconds.inWholeDays.toInt()
    binding.timeText.text = resources.getQuantityString(R.plurals.CdsTemporaryErrorBottomSheet_body1, days, days)

    binding.learnMoreButton.setOnClickListener {
      CommunicationActions.openBrowserLink(requireContext(), "https://support.signal.org/hc/articles/360007319011#android_contacts_error")
    }

    binding.settingsButton.setOnClickListener {
      val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = android.provider.ContactsContract.Contacts.CONTENT_URI
      }
      try {
        startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.CdsPermanentErrorBottomSheet_no_contacts_toast, Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    @JvmStatic
    fun show(fragmentManager: FragmentManager) {
      val fragment = CdsTemporaryErrorBottomSheet()
      fragment.show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
    }
  }
}
