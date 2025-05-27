package org.thoughtcrime.securesms.ryan.stories.my

import androidx.fragment.app.Fragment
import org.thoughtcrime.securesms.ryan.components.FragmentWrapperActivity

class MyStoriesActivity : FragmentWrapperActivity() {
  override fun getFragment(): Fragment {
    return MyStoriesFragment()
  }
}
