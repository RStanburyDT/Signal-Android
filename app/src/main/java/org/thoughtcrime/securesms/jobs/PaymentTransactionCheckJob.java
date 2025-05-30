package org.thoughtcrime.securesms.ryan.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.ryan.database.PaymentTable;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies;
import org.thoughtcrime.securesms.ryan.jobmanager.JsonJobData;
import org.thoughtcrime.securesms.ryan.jobmanager.Job;
import org.thoughtcrime.securesms.ryan.jobmanager.impl.BackoffUtil;
import org.thoughtcrime.securesms.ryan.jobmanager.impl.NetworkConstraint;
import org.thoughtcrime.securesms.ryan.payments.FailureReason;
import org.thoughtcrime.securesms.ryan.payments.PaymentTransactionId;
import org.thoughtcrime.securesms.ryan.payments.Payments;
import org.thoughtcrime.securesms.ryan.payments.Wallet;
import org.thoughtcrime.securesms.ryan.util.RemoteConfig;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class PaymentTransactionCheckJob extends BaseJob {

  private static final String TAG = Log.tag(PaymentTransactionCheckJob.class);

  public static final String KEY = "PaymentTransactionCheckJob";

  private static final String KEY_UUID = "uuid";

  private final UUID uuid;

  public PaymentTransactionCheckJob(@NonNull UUID uuid) {
    this(uuid, PaymentSendJob.QUEUE);
  }

  public PaymentTransactionCheckJob(@NonNull UUID uuid, @NonNull String queue) {
    this(new Parameters.Builder()
                       .setQueue(queue)
                       .addConstraint(NetworkConstraint.KEY)
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build(),
         uuid);
  }

  private PaymentTransactionCheckJob(@NonNull Parameters parameters, @NonNull UUID uuid) {
    super(parameters);

    this.uuid = uuid;
  }

  @Override
  protected void onRun() throws Exception {
    PaymentTable paymentDatabase = SignalDatabase.payments();

    PaymentTable.PaymentTransaction payment = paymentDatabase.getPayment(uuid);

    if (payment == null) {
      Log.w(TAG, "No payment found for UUID " + uuid);
      return;
    }

    Payments payments = AppDependencies.getPayments();

    switch (payment.getDirection()) {
      case SENT: {
        Log.i(TAG, "Checking sent status of " + uuid);
        PaymentTransactionId           paymentTransactionId = new PaymentTransactionId.MobileCoin(Objects.requireNonNull(payment.getTransaction()), Objects.requireNonNull(payment.getReceipt()), payment.getFee().requireMobileCoin());
        Wallet.TransactionStatusResult status               = payments.getWallet().getSentTransactionStatus(paymentTransactionId);

        switch (status.getTransactionStatus()) {
          case COMPLETE:
            paymentDatabase.markPaymentSuccessful(uuid, status.getBlockIndex());
            Log.i(TAG, "Marked sent payment successful " + uuid);
            break;
          case FAILED:
            paymentDatabase.markPaymentFailed(uuid, FailureReason.UNKNOWN);
            Log.i(TAG, "Marked sent payment failed " + uuid);
            break;
          case IN_PROGRESS:
            Log.i(TAG, "Sent payment still in progress " + uuid);
            throw new IncompleteTransactionException();
          default:
            throw new AssertionError();
        }
        break;
      }
      case RECEIVED: {
        Log.i(TAG, "Checking received status of " + uuid);
        Wallet.ReceivedTransactionStatus transactionStatus = payments.getWallet().getReceivedTransactionStatus(Objects.requireNonNull(payment.getReceipt()));

        switch (transactionStatus.getStatus()) {
          case COMPLETE:
            paymentDatabase.markReceivedPaymentSuccessful(uuid, transactionStatus.getAmount(), transactionStatus.getBlockIndex());
            Log.i(TAG, "Marked received payment successful " + uuid);
            break;
          case FAILED:
            paymentDatabase.markPaymentFailed(uuid, FailureReason.UNKNOWN);
            Log.i(TAG, "Marked received payment failed " + uuid);
            break;
          case IN_PROGRESS:
            Log.i(TAG, "Received payment still in progress " + uuid);
            throw new IncompleteTransactionException();
          default:
            throw new AssertionError();
        }
        break;
      }
      default: {
        throw new AssertionError();
      }
    }
  }

  @Override
  public long getNextRunAttemptBackoff(int pastAttemptCount, @NonNull Exception exception) {
    if (exception instanceof NonSuccessfulResponseCodeException) {
      if (((NonSuccessfulResponseCodeException) exception).is5xx()) {
        return BackoffUtil.exponentialBackoff(pastAttemptCount, RemoteConfig.getServerErrorMaxBackoff());
      }
    }

    if (exception instanceof IncompleteTransactionException && pastAttemptCount < 20) {
      return 500;
    }

    return super.getNextRunAttemptBackoff(pastAttemptCount, exception);
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof IncompleteTransactionException ||
           e instanceof IOException;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder()
                   .putString(KEY_UUID, uuid.toString())
                   .serialize();
  }

  @NonNull
  @Override
  public String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onFailure() {
  }

  private static final class IncompleteTransactionException extends Exception {
  }

  public static class Factory implements Job.Factory<PaymentTransactionCheckJob> {

    @Override
    public @NonNull PaymentTransactionCheckJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      return new PaymentTransactionCheckJob(parameters,
                                            UUID.fromString(data.getString(KEY_UUID)));
    }
  }
}
