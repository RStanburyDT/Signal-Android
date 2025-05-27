package org.thoughtcrime.securesms.ryan.groups.ui.chooseadmin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.thoughtcrime.securesms.ryan.groups.GroupChangeException;
import org.thoughtcrime.securesms.ryan.groups.GroupId;
import org.thoughtcrime.securesms.ryan.groups.GroupManager;
import org.thoughtcrime.securesms.ryan.groups.ui.GroupChangeFailureReason;
import org.thoughtcrime.securesms.ryan.groups.ui.GroupChangeResult;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;

import java.io.IOException;
import java.util.List;

public final class ChooseNewAdminRepository {
  private final Application context;

  ChooseNewAdminRepository(@NonNull Application context) {
    this.context = context;
  }

  @WorkerThread
  @NonNull GroupChangeResult updateAdminsAndLeave(@NonNull GroupId.V2 groupId, @NonNull List<RecipientId> newAdminIds) {
    try {
      GroupManager.addMemberAdminsAndLeaveGroup(context, groupId, newAdminIds);
      return GroupChangeResult.SUCCESS;
    } catch (GroupChangeException | IOException e) {
      return GroupChangeResult.failure(GroupChangeFailureReason.fromException(e));
    }
  }
}
