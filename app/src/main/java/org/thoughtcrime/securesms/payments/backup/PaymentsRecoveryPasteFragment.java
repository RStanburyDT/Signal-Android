package org.thoughtcrime.securesms.ryan.payments.backup;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.navigation.SafeNavigation;
import org.thoughtcrime.securesms.ryan.util.text.AfterTextChanged;
import org.whispersystems.signalservice.api.payments.PaymentsConstants;

public class PaymentsRecoveryPasteFragment extends Fragment {

  public PaymentsRecoveryPasteFragment() {
    super(R.layout.payments_recovery_paste_fragment);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Toolbar  toolbar = view.findViewById(R.id.payments_recovery_paste_fragment_toolbar);
    EditText input   = view.findViewById(R.id.payments_recovery_paste_fragment_phrase);
    View     next    = view.findViewById(R.id.payments_recovery_paste_fragment_next);

    toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

    if (savedInstanceState == null) {
      next.setEnabled(false);
    }

    input.addTextChangedListener(new AfterTextChanged(e -> {
      next.setEnabled(!e.toString().isEmpty());
      next.setAlpha(!e.toString().isEmpty() ? 1f : 0.5f);
    }));

    next.setOnClickListener(v -> {
      String   mnemonic = input.getText().toString();
      String[] words    = mnemonic.split("\\s+");

      if (words.length != PaymentsConstants.MNEMONIC_LENGTH) {
        showErrorDialog();
        return;
      }

      SafeNavigation.safeNavigate(Navigation.findNavController(v), PaymentsRecoveryPasteFragmentDirections.actionPaymentsRecoveryEntryToPaymentsRecoveryPhrase(false).setWords(words));
    });
  }

  private void showErrorDialog() {
    new MaterialAlertDialogBuilder(requireContext())
                   .setTitle(R.string.PaymentsRecoveryPasteFragment__invalid_recovery_phrase)
                   .setMessage(getString(R.string.PaymentsRecoveryPasteFragment__make_sure, PaymentsConstants.MNEMONIC_LENGTH))
                   .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                   .show();
  }
}
