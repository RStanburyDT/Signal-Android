package org.thoughtcrime.securesms.ryan.groups.ui.managegroup.dialogs;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.groups.ui.GroupMemberEntry;
import org.thoughtcrime.securesms.ryan.groups.ui.GroupMemberListView;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;

import java.util.ArrayList;
import java.util.List;

public final class GroupInviteSentDialog {

  private GroupInviteSentDialog() {
  }

  public static @Nullable Dialog showInvitesSent(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner, @NonNull List<Recipient> recipients) {
    int size = recipients.size();
    if (size == 0) {
      return null;
    }

    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context)
                                                 .setTitle(context.getResources().getQuantityString(R.plurals.GroupManagement_invitation_sent, size, size))
                                                 .setPositiveButton(android.R.string.ok, null);
    if (size == 1) {
      builder.setMessage(context.getString(R.string.GroupManagement_invite_single_user, recipients.get(0).getDisplayName(context)));
    } else {
      builder.setMessage(R.string.GroupManagement_invite_multiple_users)
             .setView(R.layout.dialog_multiple_group_invites_sent);
    }

    Dialog dialog = builder.show();
    if (size > 1) {
      GroupMemberListView invitees = dialog.findViewById(R.id.list_invitees);

      invitees.initializeAdapter(lifecycleOwner);

      List<GroupMemberEntry.PendingMember> pendingMembers = new ArrayList<>(recipients.size());
      for (Recipient r : recipients) {
        pendingMembers.add(new GroupMemberEntry.PendingMember(r));
      }

      //noinspection ConstantConditions
      invitees.setMembers(pendingMembers);
    }

    return dialog;
  }
}
