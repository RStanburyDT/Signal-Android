@file:OptIn(ExperimentalMaterial3Api::class)

package org.thoughtcrime.securesms.ryan.registration.ui.countrycode

import android.os.Bundle
import android.view.View
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.core.util.getParcelableCompat
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.compose.ComposeFragment

/**
 * Country picker fragment used in registration V1
 */
class CountryCodeFragment : ComposeFragment() {

  companion object {
    private val TAG = Log.tag(CountryCodeFragment::class.java)
    const val REQUEST_KEY_COUNTRY = "request_key_country"
    const val REQUEST_COUNTRY = "country"
    const val RESULT_COUNTRY = "country"
  }

  private val viewModel: CountryCodeViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CountryCodeSelectScreen(
      state = state,
      title = stringResource(R.string.CountryCodeFragment__your_country),
      onSearch = { search -> viewModel.filterCountries(search) },
      onDismissed = { findNavController().popBackStack() },
      onClick = { country ->
        setFragmentResult(
          REQUEST_KEY_COUNTRY,
          bundleOf(
            RESULT_COUNTRY to country
          )
        )
        findNavController().popBackStack()
      }
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val initialCountry = arguments?.getParcelableCompat(REQUEST_COUNTRY, Country::class.java)
    viewModel.loadCountries(initialCountry)
  }
}
