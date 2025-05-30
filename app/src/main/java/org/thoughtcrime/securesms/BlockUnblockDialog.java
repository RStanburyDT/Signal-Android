package org.thoughtcrime.securesms.ryan;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.concurrent.SimpleTask;
import org.signal.storageservice.protos.groups.local.DecryptedPendingMember;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.database.model.GroupRecord;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.whispersystems.signalservice.api.groupsv2.DecryptedGroupUtil;
import org.whispersystems.signalservice.api.push.ServiceId;

import java.util.List;
import java.util.Optional;

import okio.ByteString;

/**
 * This should be used whenever we want to prompt the user to block/unblock a recipient.
 */
public final class BlockUnblockDialog {

  private BlockUnblockDialog() {}

  public static void showReportSpamFor(@NonNull Context context,
                                       @NonNull Lifecycle lifecycle,
                                       @NonNull Recipient recipient,
                                       @NonNull Runnable onReportSpam,
                                       @Nullable Runnable onBlockAndReportSpam)
  {
    SimpleTask.run(lifecycle,
                   () -> buildReportSpamFor(context, recipient, onReportSpam, onBlockAndReportSpam),
                   AlertDialog.Builder::show);
  }

  public static void showBlockFor(@NonNull Context context,
                                  @NonNull Lifecycle lifecycle,
                                  @NonNull Recipient recipient,
                                  @NonNull Runnable onBlock)
  {
    SimpleTask.run(lifecycle,
                   () -> buildBlockFor(context, recipient, onBlock, null),
                   AlertDialog.Builder::show);
  }

  public static void showBlockAndReportSpamFor(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @NonNull Recipient recipient,
                                               @NonNull Runnable onBlock,
                                               @NonNull Runnable onBlockAndReportSpam)
  {
    SimpleTask.run(lifecycle,
                   () -> buildBlockFor(context, recipient, onBlock, onBlockAndReportSpam),
                   AlertDialog.Builder::show);
  }

  public static void showUnblockFor(@NonNull Context context,
                                    @NonNull Lifecycle lifecycle,
                                    @NonNull Recipient recipient,
                                    @NonNull Runnable onUnblock)
  {
    SimpleTask.run(lifecycle,
                   () -> buildUnblockFor(context, recipient, onUnblock),
                   AlertDialog.Builder::show);
  }

  @WorkerThread
  private static AlertDialog.Builder buildBlockFor(@NonNull Context context,
                                                   @NonNull Recipient recipient,
                                                   @NonNull Runnable onBlock,
                                                   @Nullable Runnable onBlockAndReportSpam)
  {
    recipient = recipient.resolve();

    AlertDialog.Builder builder   = new MaterialAlertDialogBuilder(context);
    Resources           resources = context.getResources();

    if (recipient.isGroup()) {
      if (SignalDatabase.groups().isActive(recipient.requireGroupId())) {
        builder.setTitle(resources.getString(R.string.BlockUnblockDialog_block_and_leave_s, recipient.getDisplayName(context)));
        builder.setMessage(R.string.BlockUnblockDialog_you_will_no_longer_receive_messages_or_updates);
        builder.setPositiveButton(R.string.BlockUnblockDialog_block_and_leave, ((dialog, which) -> onBlock.run()));
        builder.setNegativeButton(android.R.string.cancel, null);
      } else {
        builder.setTitle(resources.getString(R.string.BlockUnblockDialog_block_s, recipient.getDisplayName(context)));
        builder.setMessage(R.string.BlockUnblockDialog_group_members_wont_be_able_to_add_you);
        builder.setPositiveButton(R.string.RecipientPreferenceActivity_block, ((dialog, which) -> onBlock.run()));
        builder.setNegativeButton(android.R.string.cancel, null);
      }
    } else if (recipient.isReleaseNotes()) {
      builder.setTitle(resources.getString(R.string.BlockUnblockDialog_block_s, recipient.getDisplayName(context)));
      builder.setMessage(R.string.BlockUnblockDialog_block_getting_signal_updates_and_news);
      builder.setPositiveButton(R.string.BlockUnblockDialog_block, ((dialog, which) -> onBlock.run()));
      builder.setNegativeButton(android.R.string.cancel, null);
    } else {
      builder.setTitle(resources.getString(R.string.BlockUnblockDialog_block_s, recipient.getDisplayName(context)));
      builder.setMessage(recipient.isRegistered() ? R.string.BlockUnblockDialog_blocked_people_wont_be_able_to_call_you_or_send_you_messages
                                                  : R.string.BlockUnblockDialog_blocked_people_wont_be_able_to_send_you_messages);

      if (onBlockAndReportSpam != null) {
        builder.setNeutralButton(android.R.string.cancel, null);
        builder.setNegativeButton(R.string.BlockUnblockDialog_report_spam_and_block, (d, w) -> onBlockAndReportSpam.run());
        builder.setPositiveButton(R.string.BlockUnblockDialog_block, (d, w) -> onBlock.run());
      } else {
        builder.setPositiveButton(R.string.BlockUnblockDialog_block, ((dialog, which) -> onBlock.run()));
        builder.setNegativeButton(android.R.string.cancel, null);
      }
    }

    return builder;
  }

  @WorkerThread
  private static AlertDialog.Builder buildUnblockFor(@NonNull Context context,
                                                     @NonNull Recipient recipient,
                                                     @NonNull Runnable onUnblock)
  {
    recipient = recipient.resolve();

    AlertDialog.Builder builder   = new MaterialAlertDialogBuilder(context);
    Resources           resources = context.getResources();

    if (recipient.isGroup()) {
      if (SignalDatabase.groups().isActive(recipient.requireGroupId())) {
        builder.setTitle(resources.getString(R.string.BlockUnblockDialog_unblock_s, recipient.getDisplayName(context)));
        builder.setMessage(R.string.BlockUnblockDialog_group_members_will_be_able_to_add_you);
        builder.setPositiveButton(R.string.RecipientPreferenceActivity_unblock, ((dialog, which) -> onUnblock.run()));
        builder.setNegativeButton(android.R.string.cancel, null);
      } else {
        builder.setTitle(resources.getString(R.string.BlockUnblockDialog_unblock_s, recipient.getDisplayName(context)));
        builder.setMessage(R.string.BlockUnblockDialog_group_members_will_be_able_to_add_you);
        builder.setPositiveButton(R.string.RecipientPreferenceActivity_unblock, ((dialog, which) -> onUnblock.run()));
        builder.setNegativeButton(android.R.string.cancel, null);
      }
    } else if (recipient.isReleaseNotes()) {
      builder.setTitle(resources.getString(R.string.BlockUnblockDialog_unblock_s, recipient.getDisplayName(context)));
      builder.setMessage(R.string.BlockUnblockDialog_resume_getting_signal_updates_and_news);

      builder.setPositiveButton(R.string.RecipientPreferenceActivity_unblock, ((dialog, which) -> onUnblock.run()));
      builder.setNegativeButton(android.R.string.cancel, null);
    } else {
      builder.setTitle(resources.getString(R.string.BlockUnblockDialog_unblock_s, recipient.getDisplayName(context)));
      builder.setMessage(recipient.isRegistered() ? R.string.BlockUnblockDialog_you_will_be_able_to_call_and_message_each_other
                                                  : R.string.BlockUnblockDialog_you_will_be_able_to_message_each_other);
      builder.setPositiveButton(R.string.RecipientPreferenceActivity_unblock, ((dialog, which) -> onUnblock.run()));
      builder.setNegativeButton(android.R.string.cancel, null);
    }

    return builder;
  }

  @WorkerThread
  private static AlertDialog.Builder buildReportSpamFor(@NonNull Context context,
                                                        @NonNull Recipient recipient,
                                                        @NonNull Runnable onReportSpam,
                                                        @Nullable Runnable onBlockAndReportSpam)
  {
    recipient = recipient.resolve();

    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.BlockUnblockDialog_report_spam_title)
        .setPositiveButton(R.string.BlockUnblockDialog_report_spam, (d, w) -> onReportSpam.run());

    if (onBlockAndReportSpam != null) {
      builder.setNeutralButton(android.R.string.cancel, null)
             .setNegativeButton(R.string.BlockUnblockDialog_report_spam_and_block, (d, w) -> onBlockAndReportSpam.run());
    } else {
      builder.setNegativeButton(android.R.string.cancel, null);
    }

    if (recipient.isGroup()) {
      Recipient adder = SignalDatabase.groups().getGroupInviter(recipient.requireGroupId());
      if (adder != null) {
        builder.setMessage(context.getString(R.string.BlockUnblockDialog_report_spam_group_named_adder, adder.getDisplayName(context)));
      } else {
        builder.setMessage(R.string.BlockUnblockDialog_report_spam_group_unknown_adder);
      }
    } else {
      builder.setMessage(R.string.BlockUnblockDialog_report_spam_description);
    }

    return builder;
  }
}
