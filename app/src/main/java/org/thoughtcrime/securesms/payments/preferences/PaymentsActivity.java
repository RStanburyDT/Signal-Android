package org.thoughtcrime.securesms.ryan.payments.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.thoughtcrime.securesms.ryan.PassphraseRequiredActivity;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.jobs.PaymentLedgerUpdateJob;
import org.thoughtcrime.securesms.ryan.payments.preferences.details.PaymentDetailsFragmentArgs;
import org.thoughtcrime.securesms.ryan.payments.preferences.details.PaymentDetailsParcelable;
import org.thoughtcrime.securesms.ryan.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.ryan.util.DynamicTheme;
import org.thoughtcrime.securesms.ryan.util.navigation.SafeNavigation;

import java.util.UUID;

public class PaymentsActivity extends PassphraseRequiredActivity {

  public static final String EXTRA_PAYMENTS_STARTING_ACTION = "payments_starting_action";
  public static final String EXTRA_STARTING_ARGUMENTS       = "payments_starting_arguments";

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  public static Intent navigateToPaymentDetails(@NonNull Context context, @NonNull UUID paymentId) {
    Intent intent = new Intent(context, PaymentsActivity.class);

    intent.putExtra(EXTRA_PAYMENTS_STARTING_ACTION, R.id.action_directly_to_paymentDetails);
    intent.putExtra(EXTRA_STARTING_ARGUMENTS, new PaymentDetailsFragmentArgs.Builder(PaymentDetailsParcelable.forUuid(paymentId)).build().toBundle());

    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState, boolean ready) {
    dynamicTheme.onCreate(this);

    setContentView(R.layout.payments_activity);

    NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment);
    controller.setGraph(R.navigation.payments_preferences);

    int startingAction = getIntent().getIntExtra(EXTRA_PAYMENTS_STARTING_ACTION, R.id.paymentsHome);
    if (startingAction != R.id.paymentsHome) {
      SafeNavigation.safeNavigate(controller, startingAction, getIntent().getBundleExtra(EXTRA_STARTING_ARGUMENTS));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    dynamicTheme.onResume(this);

    AppDependencies.getJobManager()
                   .add(PaymentLedgerUpdateJob.updateLedger());
  }
}
