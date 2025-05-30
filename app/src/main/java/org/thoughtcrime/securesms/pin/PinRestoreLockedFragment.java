package org.thoughtcrime.securesms.ryan.pin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.ryan.LoggingFragment;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.CommunicationActions;

public class PinRestoreLockedFragment extends LoggingFragment {

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.pin_restore_locked_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    View createPinButton = view.findViewById(R.id.pin_locked_next);
    View learnMoreButton = view.findViewById(R.id.pin_locked_learn_more);

    createPinButton.setOnClickListener(v -> {
      SvrRepository.onPinRestoreForgottenOrSkipped();
      ((PinRestoreActivity) requireActivity()).navigateToPinCreation();
    });

    learnMoreButton.setOnClickListener(v -> {
      CommunicationActions.openBrowserLink(requireContext(), getString(R.string.PinRestoreLockedFragment_learn_more_url));
    });
  }
}
