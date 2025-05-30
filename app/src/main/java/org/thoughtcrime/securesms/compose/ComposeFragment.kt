package org.thoughtcrime.securesms.ryan.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.compose.content
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.ryan.LoggingFragment
import org.thoughtcrime.securesms.ryan.util.DynamicTheme

/**
 * Generic ComposeFragment which can be subclassed to build UI with compose.
 */
abstract class ComposeFragment : LoggingFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = content {
    SignalTheme(
      isDarkMode = DynamicTheme.isDarkTheme(LocalContext.current)
    ) {
      FragmentContent()
    }
  }

  @Composable
  abstract fun FragmentContent()
}
