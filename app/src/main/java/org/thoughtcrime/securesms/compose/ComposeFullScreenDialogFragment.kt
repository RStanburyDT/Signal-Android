package org.thoughtcrime.securesms.ryan.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.util.DynamicTheme

/**
 * Generic ComposeFragment which can be subclassed to build UI with compose.
 */
abstract class ComposeFullScreenDialogFragment : DialogFragment() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NO_FRAME, R.style.Signal_DayNight_Dialog_FullScreen)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        SignalTheme(
          isDarkMode = DynamicTheme.isDarkTheme(LocalContext.current)
        ) {
          DialogContent()
        }
      }
    }
  }

  @Composable
  abstract fun DialogContent()
}
