package org.thoughtcrime.securesms.ryan.database.model;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import org.signal.core.util.BidiUtil;
import org.signal.storageservice.protos.groups.AccessControl;
import org.signal.storageservice.protos.groups.Member;
import org.signal.storageservice.protos.groups.local.DecryptedApproveMember;
import org.signal.storageservice.protos.groups.local.DecryptedGroup;
import org.signal.storageservice.protos.groups.local.DecryptedGroupChange;
import org.signal.storageservice.protos.groups.local.DecryptedMember;
import org.signal.storageservice.protos.groups.local.DecryptedModifyMemberRole;
import org.signal.storageservice.protos.groups.local.DecryptedPendingMember;
import org.signal.storageservice.protos.groups.local.DecryptedPendingMemberRemoval;
import org.signal.storageservice.protos.groups.local.DecryptedRequestingMember;
import org.signal.storageservice.protos.groups.local.EnabledState;
import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GenericGroupUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupAdminStatusUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupAnnouncementOnlyChangeUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupAttributesAccessLevelChangeUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupAvatarUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupChangeChatUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupCreationUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupDescriptionUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupExpirationTimerUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInvitationAcceptedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInvitationDeclinedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInvitationRevokedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInviteLinkAdminApprovalUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInviteLinkDisabledUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInviteLinkEnabledUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupInviteLinkResetUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupJoinRequestApprovalUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupJoinRequestCanceledUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupJoinRequestUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMemberAddedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMemberJoinedByLinkUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMemberJoinedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMemberLeftUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMemberRemovedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupMembershipAccessLevelChangeUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupNameUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupSelfInvitationRevokedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupUnknownInviteeUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupV2AccessLevel;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupV2MigrationDroppedMembersUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupV2MigrationInvitedMembersUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupV2MigrationSelfInvitedUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.GroupV2MigrationUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.SelfInvitedOtherUserToGroupUpdate;
import org.thoughtcrime.securesms.ryan.backup.v2.proto.SelfInvitedToGroupUpdate;
import org.thoughtcrime.securesms.ryan.fonts.SignalSymbols.Glyph;
import org.thoughtcrime.securesms.ryan.groups.GV2AccessLevelUtil;
import org.thoughtcrime.securesms.ryan.recipients.Recipient;
import org.thoughtcrime.securesms.ryan.recipients.RecipientId;
import org.thoughtcrime.securesms.ryan.util.ExpirationUtil;
import org.thoughtcrime.securesms.ryan.util.SpanUtil;
import org.whispersystems.signalservice.api.groupsv2.DecryptedGroupUtil;
import org.whispersystems.signalservice.api.push.ServiceId;
import org.whispersystems.signalservice.api.push.ServiceIds;
import org.whispersystems.signalservice.api.util.UuidUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import okio.ByteString;

final class GroupsV2UpdateMessageProducer {

  @NonNull private final  Context               context;
  @NonNull private final  ServiceIds            selfIds;
  @Nullable private final Consumer<RecipientId> recipientClickHandler;

  GroupsV2UpdateMessageProducer(@NonNull Context context, @NonNull ServiceIds selfIds, @Nullable Consumer<RecipientId> recipientClickHandler) {
    this.context               = context;
    this.selfIds               = selfIds;
    this.recipientClickHandler = recipientClickHandler;
  }

  /**
   * Describes a group that is new to you, use this when there is no available change record.
   * <p>
   * Invitation and revision 0 groups are the most common use cases for this.
   * <p>
   * When invited, it's possible there's no change available.
   * <p>
   * When the revision of the group is 0, the change is very noisy and only the editor is useful.
   */
  UpdateDescription describeNewGroup(@Nullable DecryptedGroup group, @Nullable DecryptedGroupChange decryptedGroupChange) {
    Optional<DecryptedPendingMember> selfPending = Optional.empty();

    if (group != null) {
      selfPending = DecryptedGroupUtil.findPendingByServiceId(group.pendingMembers, selfIds.getAci());
      if (selfPending.isEmpty() && selfIds.getPni() != null) {
        selfPending = DecryptedGroupUtil.findPendingByServiceId(group.pendingMembers, selfIds.getPni());
      }
    }

    if (selfPending.isPresent()) {
      return updateDescription(R.string.MessageRecord_s_invited_you_to_the_group, selfPending.get().addedByAci, Glyph.PERSON_PLUS);
    }

    if (decryptedGroupChange != null) {
      ByteString foundingMemberUuid = decryptedGroupChange.editorServiceIdBytes;
      if (foundingMemberUuid.size() > 0) {
        if (selfIds.matches(foundingMemberUuid)) {
          return updateDescription(context.getString(R.string.MessageRecord_you_created_the_group), Glyph.GROUP);
        } else {
          return updateDescription(R.string.MessageRecord_s_added_you, foundingMemberUuid, Glyph.PERSON_PLUS);
        }
      }
    }

    if (group != null && DecryptedGroupUtil.findMemberByAci(group.members, selfIds.getAci()).isPresent()) {
      return updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS);
    } else {
      return updateDescription(context.getString(R.string.MessageRecord_group_updated), Glyph.GROUP);
    }
  }

  List<UpdateDescription> describeChanges(@NonNull List<GroupChangeChatUpdate.Update> groupUpdates) {
    List<UpdateDescription> updates = new LinkedList<>();
    for (GroupChangeChatUpdate.Update update : groupUpdates) {
      describeUpdate(update, updates);
    }
    if (updates.isEmpty()) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_group_updated), Glyph.GROUP));
    }

    return updates;
  }

  private void describeUpdate(@NonNull GroupChangeChatUpdate.Update update, @NonNull List<UpdateDescription> updates) {
    if (update.genericGroupUpdate != null) {
      describeGenericGroupUpdate(update.genericGroupUpdate, updates);
    } else if (update.groupCreationUpdate != null) {
      describeGroupCreationUpdate(update.groupCreationUpdate, updates);
    } else if (update.groupNameUpdate != null) {
      describeGroupNameUpdate(update.groupNameUpdate, updates);
    } else if (update.groupAvatarUpdate != null) {
      describeAvatarChange(update.groupAvatarUpdate, updates);
    } else if (update.groupDescriptionUpdate != null) {
      describeDescriptionChange(update.groupDescriptionUpdate, updates);
    } else if (update.groupMembershipAccessLevelChangeUpdate != null) {
      describeGroupMembershipAccessLevelChange(update.groupMembershipAccessLevelChangeUpdate, updates);
    } else if (update.groupAttributesAccessLevelChangeUpdate != null) {
      describeGroupAttributesAccessLevelChange(update.groupAttributesAccessLevelChangeUpdate, updates);
    } else if (update.groupAnnouncementOnlyChangeUpdate != null) {
      describeGroupAnnouncementOnlyUpdate(update.groupAnnouncementOnlyChangeUpdate, updates);
    } else if (update.groupAdminStatusUpdate != null) {
      describeAdminStatusChange(update.groupAdminStatusUpdate, updates);
    } else if (update.groupMemberLeftUpdate != null) {
      describeGroupMemberLeftChange(update.groupMemberLeftUpdate, updates);
    } else if (update.groupMemberRemovedUpdate != null) {
      describeGroupMemberRemovedChange(update.groupMemberRemovedUpdate, updates);
    } else if (update.selfInvitedToGroupUpdate != null) {
      describeSelfInvitedToGroupUpdate(update.selfInvitedToGroupUpdate, updates);
    } else if (update.selfInvitedOtherUserToGroupUpdate != null) {
      describeSelfInvitedOtherUserToGroupUpdate(update.selfInvitedOtherUserToGroupUpdate, updates);
    } else if (update.groupUnknownInviteeUpdate != null) {
      describeUnknownUsersInvitedUpdate(update.groupUnknownInviteeUpdate, updates);
    } else if (update.groupInvitationAcceptedUpdate != null) {
      describeGroupInvitationAcceptedUpdate(update.groupInvitationAcceptedUpdate, updates);
    } else if (update.groupMemberJoinedUpdate != null) {
      describeGroupMemberJoinedUpdate(update.groupMemberJoinedUpdate, updates);
    } else if (update.groupMemberAddedUpdate != null) {
      describeGroupMemberAddedUpdate(update.groupMemberAddedUpdate, updates);
    } else if (update.groupInvitationDeclinedUpdate != null) {
      describeGroupInvitationDeclinedUpdate(update.groupInvitationDeclinedUpdate, updates);
    } else if (update.groupInvitationRevokedUpdate != null) {
      describeGroupInvitationRevokedUpdate(update.groupInvitationRevokedUpdate, updates);
    } else if (update.groupJoinRequestUpdate != null) {
      describeGroupJoinRequestUpdate(update.groupJoinRequestUpdate, updates);
    } else if (update.groupJoinRequestApprovalUpdate != null) {
      describeGroupJoinRequestApprovedUpdate(update.groupJoinRequestApprovalUpdate, updates);
    } else if (update.groupJoinRequestCanceledUpdate != null) {
      describeGroupJoinRequestCanceledUpdate(update.groupJoinRequestCanceledUpdate, updates);
    } else if (update.groupInviteLinkResetUpdate != null) {
      describeInviteLinkResetUpdate(update.groupInviteLinkResetUpdate, updates);
    } else if (update.groupInviteLinkEnabledUpdate != null) {
      describeInviteLinkEnabledUpdate(update.groupInviteLinkEnabledUpdate, updates);
    } else if (update.groupInviteLinkDisabledUpdate != null) {
      describeInviteLinkDisabledUpdate(update.groupInviteLinkDisabledUpdate, updates);
    } else if (update.groupInviteLinkAdminApprovalUpdate != null) {
      describeGroupInviteLinkAdminApprovalUpdate(update.groupInviteLinkAdminApprovalUpdate, updates);
    } else if (update.groupV2MigrationUpdate != null) {
      describeGroupV2MigrationUpdate(update.groupV2MigrationUpdate, updates);
    } else if (update.groupV2MigrationDroppedMembersUpdate != null) {
      describeGroupV2MigrationDroppedMembersUpdate(update.groupV2MigrationDroppedMembersUpdate, updates);
    } else if (update.groupV2MigrationInvitedMembersUpdate != null) {
      describeGroupV2MigrationInvitedMembersUpdate(update.groupV2MigrationInvitedMembersUpdate, updates);
    } else if (update.groupV2MigrationSelfInvitedUpdate != null) {
      describeGroupV2MigrationSelfInvitedUpdate(update.groupV2MigrationSelfInvitedUpdate, updates);
    } else if (update.groupMemberJoinedByLinkUpdate != null) {
      describeGroupMemberJoinedByLinkUpdate(update.groupMemberJoinedByLinkUpdate, updates);
    } else if (update.groupExpirationTimerUpdate != null) {
      describeGroupExpirationTimerUpdate(update.groupExpirationTimerUpdate, updates);
    } else if (update.groupSelfInvitationRevokedUpdate != null) {
      describeGroupSelfInvitationRevokedUpdate(update.groupSelfInvitationRevokedUpdate, updates);
    }
  }

  private void describeGroupSelfInvitationRevokedUpdate(@NonNull GroupSelfInvitationRevokedUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.revokerAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_an_admin_revoked_your_invitation_to_the_group), Glyph.PERSON_X));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_revoked_your_invitation_to_the_group, update.revokerAci, Glyph.PERSON_X));
    }
  }

  private void describeGroupExpirationTimerUpdate(@NonNull GroupExpirationTimerUpdate update, @NonNull List<UpdateDescription> updates) {
    final int duration = Math.toIntExact(update.expiresInMs / 1000);
    String    time     = ExpirationUtil.getExpirationDisplayValue(context, duration);
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_disappearing_message_time_set_to_s, time), Glyph.TIMER));
    } else {
      boolean editorIsYou = selfIds.matches(update.updaterAci);
      if (duration <= 0) {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_disabled_disappearing_messages), Glyph.TIMER_SLASH));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_disabled_disappearing_messages, update.updaterAci, Glyph.TIMER_SLASH));
        }
      } else {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_set_disappearing_message_time_to_s, time), Glyph.TIMER));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_set_disappearing_message_time_to_s, update.updaterAci, time, Glyph.TIMER));
        }
      }
    }
  }

  private void describeGroupMemberJoinedByLinkUpdate(@NonNull GroupMemberJoinedByLinkUpdate update, @NonNull List<UpdateDescription> updates) {
    if (selfIds.matches(update.newMemberAci)) {
      updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group_via_the_group_link), Glyph.PERSON_CHECK));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group_via_the_group_link, update.newMemberAci, Glyph.PERSON_CHECK));
    }
  }

  private void describeGroupV2MigrationSelfInvitedUpdate(@NonNull GroupV2MigrationSelfInvitedUpdate update, @NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(context.getString(R.string.MessageRecord_you_couldnt_be_added_to_the_new_group_and_have_been_invited_to_join), Glyph.PERSON_PLUS));
  }

  private void describeGroupV2MigrationDroppedMembersUpdate(@NonNull GroupV2MigrationDroppedMembersUpdate update, @NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(context.getResources()
                                         .getQuantityString(R.plurals.MessageRecord_members_couldnt_be_added_to_the_new_group_and_have_been_removed, update.droppedMembersCount, update.droppedMembersCount), Glyph.PERSON_MINUS));
  }

  private void describeGroupV2MigrationInvitedMembersUpdate(@NonNull GroupV2MigrationInvitedMembersUpdate update, @NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(context.getResources()
                                         .getQuantityString(R.plurals.MessageRecord_members_couldnt_be_added_to_the_new_group_and_have_been_invited, update.invitedMembersCount, update.invitedMembersCount), Glyph.PERSON_MINUS));
  }

  private void describeGroupV2MigrationUpdate(@NonNull GroupV2MigrationUpdate update, @NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(context.getString(R.string.MessageRecord_this_group_was_updated_to_a_new_group), Glyph.MEGAPHONE));
  }

  private void describeGroupInviteLinkAdminApprovalUpdate(@NonNull GroupInviteLinkAdminApprovalUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      if (update.linkRequiresAdminApproval) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_the_admin_approval_for_the_group_link_has_been_turned_on), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_the_admin_approval_for_the_group_link_has_been_turned_off), Glyph.MEGAPHONE));
      }
    } else {
      if (selfIds.matches(update.updaterAci)) {
        if (update.linkRequiresAdminApproval) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_admin_approval_for_the_group_link), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_off_admin_approval_for_the_group_link), Glyph.MEGAPHONE));
        }
      } else {
        if (update.linkRequiresAdminApproval) {
          updates.add(updateDescription(R.string.MessageRecord_s_turned_on_admin_approval_for_the_group_link, update.updaterAci, Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_turned_off_admin_approval_for_the_group_link, update.updaterAci, Glyph.MEGAPHONE));
        }
      }
    }
  }

  private void describeInviteLinkDisabledUpdate(@NonNull GroupInviteLinkDisabledUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_off), Glyph.MEGAPHONE));
    } else {
      if (selfIds.matches(update.updaterAci)) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_off_the_group_link), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_turned_off_the_group_link, update.updaterAci, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeInviteLinkEnabledUpdate(@NonNull GroupInviteLinkEnabledUpdate update, @NonNull List<UpdateDescription> updates) {

    if (update.updaterAci == null) {
      if (update.linkRequiresAdminApproval) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_on_with_admin_approval_on), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_on_with_admin_approval_off), Glyph.MEGAPHONE));
      }
    } else {
      if (selfIds.matches(update.updaterAci)) {
        if (update.linkRequiresAdminApproval) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_the_group_link_with_admin_approval_on), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_the_group_link_with_admin_approval_off), Glyph.MEGAPHONE));
        }
      } else {
        if (update.linkRequiresAdminApproval) {
          updates.add(updateDescription(R.string.MessageRecord_s_turned_on_the_group_link_with_admin_approval_on, update.updaterAci, Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_turned_on_the_group_link_with_admin_approval_off, update.updaterAci, Glyph.MEGAPHONE));
        }
      }
    }
  }

  private void describeInviteLinkResetUpdate(@NonNull GroupInviteLinkResetUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_reset), Glyph.MEGAPHONE));
    } else {
      if (selfIds.matches(update.updaterAci)) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_reset_the_group_link), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_reset_the_group_link, update.updaterAci, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeGroupJoinRequestCanceledUpdate(@NonNull GroupJoinRequestCanceledUpdate update, @NonNull List<UpdateDescription> updates) {
    boolean requestingMemberIsYou = selfIds.matches(update.requestorAci);

    if (requestingMemberIsYou) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_canceled_your_request_to_join_the_group), Glyph.PERSON_X));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_canceled_their_request_to_join_the_group, update.requestorAci, Glyph.PERSON_X));
    }
  }

  private void describeGroupJoinRequestApprovedUpdate(@NonNull GroupJoinRequestApprovalUpdate update, @NonNull List<UpdateDescription> updates) {
    boolean requestingMemberIsYou = selfIds.matches(update.requestorAci);

    if (update.wasApproved) {
      if (update.updaterAci == null) {
        if (requestingMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_approved), Glyph.PERSON_CHECK));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_a_request_to_join_the_group_from_s_has_been_approved, update.requestorAci, Glyph.PERSON_CHECK));
        }
      } else {
        if (requestingMemberIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_s_approved_your_request_to_join_the_group, update.updaterAci, Glyph.PERSON_CHECK));
        } else {
          boolean editorIsYou = selfIds.matches(update.updaterAci);

          if (editorIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_you_approved_a_request_to_join_the_group_from_s, update.requestorAci, Glyph.PERSON_CHECK));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_approved_a_request_to_join_the_group_from_s, update.updaterAci, update.requestorAci, Glyph.PERSON_CHECK));
          }
        }
      }
    } else {
      if (update.updaterAci == null) {
        if (requestingMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_denied_by_an_admin), Glyph.PERSON_X));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_a_request_to_join_the_group_from_s_has_been_denied, update.requestorAci, Glyph.PERSON_X));
        }
      } else {
        if (requestingMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_denied_by_an_admin), Glyph.PERSON_X));
        } else {
          boolean editorIsYou = selfIds.matches(update.updaterAci);

          if (editorIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_you_denied_a_request_to_join_the_group_from_s, update.requestorAci, Glyph.PERSON_X));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_denied_a_request_to_join_the_group_from_s, update.updaterAci, update.requestorAci, Glyph.PERSON_X));
          }
        }
      }
    }
  }

  private void describeGroupJoinRequestUpdate(@NonNull GroupJoinRequestUpdate update, @NonNull List<UpdateDescription> updates) {
    if (selfIds.matches(update.requestorAci)) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_sent_a_request_to_join_the_group), Glyph.GROUP));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_requested_to_join_via_the_group_link, update.requestorAci, Glyph.GROUP));
    }
  }

  private void describeGroupInvitationRevokedUpdate(@NonNull GroupInvitationRevokedUpdate update, @NonNull List<UpdateDescription> updates) {
    int revokedMeCount = 0;
    for (GroupInvitationRevokedUpdate.Invitee invitee : update.invitees) {
      if ((invitee.inviteeAci != null && selfIds.matches(invitee.inviteeAci)) || (invitee.inviteePni != null && selfIds.matches(invitee.inviteePni))) {
        revokedMeCount++;
      }
    }

    int notMeInvitees = update.invitees.size() - revokedMeCount;

    if (update.updaterAci == null) {
      if (revokedMeCount > 0) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_an_admin_revoked_your_invitation_to_the_group), Glyph.PERSON_X));
      }
      if (notMeInvitees > 0) {
        updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_d_invitations_were_revoked, notMeInvitees, notMeInvitees), Glyph.PERSON_X));
      }
    } else {
      if (revokedMeCount > 0) {
        updates.add(updateDescription(R.string.MessageRecord_s_revoked_your_invitation_to_the_group, update.updaterAci, Glyph.PERSON_X));
      }
      if (selfIds.matches(update.updaterAci)) {
        updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_you_revoked_invites, notMeInvitees, notMeInvitees), Glyph.PERSON_X));
      } else {
        updates.add(updateDescription(R.plurals.MessageRecord_s_revoked_invites, notMeInvitees, update.updaterAci, notMeInvitees, Glyph.PERSON_X));
      }
    }
  }

  private void describeGroupInvitationDeclinedUpdate(@NonNull GroupInvitationDeclinedUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.inviteeAci != null && selfIds.matches(update.inviteeAci)) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_declined_the_invitation_to_the_group), Glyph.PERSON_X));
    } else {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_someone_declined_an_invitation_to_the_group), Glyph.PERSON_X));
    }
  }

  private void describeGroupMemberAddedUpdate(@NonNull GroupMemberAddedUpdate update, @NonNull List<UpdateDescription> updates) {
    boolean newMemberIsYou = selfIds.matches(update.newMemberAci);

    if (update.updaterAci == null) {
      if (newMemberIsYou) {
        updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group, update.newMemberAci, Glyph.PERSON_PLUS));
      }
    } else {
      if (newMemberIsYou) {
        updates.add(0, updateDescription(R.string.MessageRecord_s_added_you, update.updaterAci, Glyph.PERSON_PLUS));
      } else if (selfIds.matches(update.updaterAci)) {
        if (update.hadOpenInvitation) {
          updates.add(updateDescription(R.string.MessageRecord_you_added_invited_member_s, update.newMemberAci, Glyph.PERSON_PLUS));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_added_s, update.newMemberAci, Glyph.PERSON_PLUS));
        }
      } else {
        if (update.hadOpenInvitation) {
          updates.add(updateDescription(R.string.MessageRecord_s_added_invited_member_s, update.updaterAci, update.newMemberAci, Glyph.PERSON_PLUS));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_added_s, update.updaterAci, update.newMemberAci, Glyph.PERSON_PLUS));
        }
      }
    }
  }

  private void describeGroupMemberJoinedUpdate(@NonNull GroupMemberJoinedUpdate update, @NonNull List<UpdateDescription> updates) {
    boolean newMemberIsYou = selfIds.matches(update.newMemberAci);

    if (newMemberIsYou) {
      updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group, update.newMemberAci, Glyph.PERSON_PLUS));
    }
  }

  private void describeGroupInvitationAcceptedUpdate(@NonNull GroupInvitationAcceptedUpdate update, @NonNull List<UpdateDescription> updates) {
    if (selfIds.matches(update.newMemberAci)) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_accepted_invite), Glyph.PERSON_CHECK));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_accepted_invite, update.newMemberAci, Glyph.PERSON_CHECK));
    }
  }

  private void describeUnknownUsersInvitedUpdate(@NonNull GroupUnknownInviteeUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.inviterAci == null) {
      updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_d_people_were_invited_to_the_group, update.inviteeCount, update.inviteeCount), Glyph.PERSON_PLUS));
    } else {
      updates.add(updateDescription(R.plurals.MessageRecord_s_invited_members, update.inviteeCount, update.inviterAci, update.inviteeCount, Glyph.PERSON_PLUS));
    }
  }

  private void describeSelfInvitedOtherUserToGroupUpdate(@NonNull SelfInvitedOtherUserToGroupUpdate update, @NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(R.string.MessageRecord_you_invited_s_to_the_group, update.inviteeServiceId, Glyph.PERSON_PLUS));
  }

  private void describeSelfInvitedToGroupUpdate(@NonNull SelfInvitedToGroupUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.inviterAci == null) {
      updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_were_invited_to_the_group), Glyph.PERSON_PLUS));
    } else {
      updates.add(0, updateDescription(R.string.MessageRecord_s_invited_you_to_the_group, update.inviterAci, Glyph.PERSON_PLUS));
    }
  }

  private void describeGenericGroupUpdate(@NonNull GenericGroupUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_was_updated), Glyph.GROUP));
    } else {
      boolean editorIsYou = selfIds.matches(update.updaterAci);

      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_updated_group), Glyph.GROUP));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_updated_group, update.updaterAci, Glyph.GROUP));
      }
    }
  }

  private void describeGroupCreationUpdate(@NonNull GroupCreationUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_group_updated), Glyph.GROUP));
    } else {
      if (selfIds.matches(update.updaterAci)) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_created_the_group), Glyph.GROUP));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_added_you, update.updaterAci, Glyph.PERSON_PLUS));
      }
    }
  }

  private void describeGroupNameUpdate(@NonNull GroupNameUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_name_has_changed_to_s, BidiUtil.isolateBidi(update.newGroupName)), Glyph.EDIT));
    } else {
      String newTitle = BidiUtil.isolateBidi(update.newGroupName);
      if (selfIds.matches(update.updaterAci)) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_name_to_s, newTitle), Glyph.EDIT));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_name_to_s, update.updaterAci, newTitle, Glyph.EDIT));
      }
    }
  }

  private void describeGroupMembershipAccessLevelChange(@NonNull GroupMembershipAccessLevelChangeUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.accessLevel == GroupV2AccessLevel.UNKNOWN) {
      return;
    }
    String accessLevel = GV2AccessLevelUtil.toString(context, backupGv2AccessLevelToGroups(update.accessLevel));
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_who_can_edit_group_membership_has_been_changed_to_s, accessLevel), Glyph.MEGAPHONE));
    } else {
      boolean editorIsYou = selfIds.matches(update.updaterAci);

      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_who_can_edit_group_membership_to_s, accessLevel), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_who_can_edit_group_membership_to_s, update.updaterAci, accessLevel, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeGroupAttributesAccessLevelChange(@NonNull GroupAttributesAccessLevelChangeUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.accessLevel == GroupV2AccessLevel.UNKNOWN) {
      return;
    }
    String accessLevel = GV2AccessLevelUtil.toString(context, backupGv2AccessLevelToGroups(update.accessLevel));
    if (update.updaterAci == null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_who_can_edit_group_info_has_been_changed_to_s, accessLevel), Glyph.MEGAPHONE));
    } else {
      boolean editorIsYou = selfIds.matches(update.updaterAci);

      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_who_can_edit_group_info_to_s, accessLevel), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_who_can_edit_group_info_to_s, update.updaterAci, accessLevel, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeGroupAnnouncementOnlyUpdate(@NonNull GroupAnnouncementOnlyChangeUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.updaterAci == null) {
      if (update.isAnnouncementOnly) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_allow_only_admins_to_send), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_allow_all_members_to_send), Glyph.MEGAPHONE));
      }
    } else {
      boolean editorIsYou = selfIds.matches(update.updaterAci);

      if (update.isAnnouncementOnly) {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_allow_only_admins_to_send), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_allow_only_admins_to_send, update.updaterAci, Glyph.MEGAPHONE));
        }
      } else {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_allow_all_members_to_send), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_allow_all_members_to_send, update.updaterAci, Glyph.MEGAPHONE));
        }
      }
    }
  }

  private void describeGroupMemberLeftChange(@NonNull GroupMemberLeftUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.aci == null) {
      return;
    }
    boolean editorIsYou = selfIds.matches(update.aci);
    if (editorIsYou) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_left_the_group), Glyph.LEAVE));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_left_the_group, update.aci, Glyph.LEAVE));
    }
  }

  private void describeGroupMemberRemovedChange(@NonNull GroupMemberRemovedUpdate update, @NonNull List<UpdateDescription> updates) {
    if (update.removerAci == null) {
      boolean removedMemberIsYou = selfIds.matches(update.removedAci);

      if (removedMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_no_longer_in_the_group), Glyph.LEAVE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_is_no_longer_in_the_group, update.removedAci, Glyph.LEAVE));
      }
    } else {
      boolean editorIsYou = selfIds.matches(update.removerAci);

      boolean removedMemberIsYou = selfIds.matches(update.removedAci);

      if (editorIsYou) {
        if (removedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_left_the_group), Glyph.LEAVE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_removed_s, update.removedAci, Glyph.PERSON_MINUS));
        }
      } else {
        if (removedMemberIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_s_removed_you_from_the_group, update.removerAci, Glyph.PERSON_MINUS));
        } else {
          if (update.removerAci.equals(update.removedAci)) {
            updates.add(updateDescription(R.string.MessageRecord_s_left_the_group, update.removedAci, Glyph.LEAVE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_removed_s, update.removerAci, update.removedAci, Glyph.PERSON_MINUS));
          }
        }
      }
    }
  }

  private AccessControl.AccessRequired backupGv2AccessLevelToGroups(@NonNull GroupV2AccessLevel accessLevel) {
    switch (accessLevel) {
      case ANY:
        return AccessControl.AccessRequired.ANY;
      case MEMBER:
        return AccessControl.AccessRequired.MEMBER;
      case ADMINISTRATOR:
        return AccessControl.AccessRequired.ADMINISTRATOR;
      case UNSATISFIABLE:
        return AccessControl.AccessRequired.UNSATISFIABLE;
      default:
      case UNKNOWN:
        return AccessControl.AccessRequired.UNKNOWN;
    }
  }

  List<UpdateDescription> describeChanges(@Nullable DecryptedGroup previousGroupState, @NonNull DecryptedGroupChange change) {
    if (new DecryptedGroup().equals(previousGroupState)) {
      previousGroupState = null;
    }

    List<UpdateDescription> updates = new LinkedList<>();

    boolean   editorUnknown   = change.editorServiceIdBytes.size() == 0;
    ServiceId editorServiceId = editorUnknown ? null : ServiceId.parseOrNull(change.editorServiceIdBytes);

    if (editorServiceId == null || editorServiceId.isUnknown()) {
      editorUnknown = true;
    }

    if (editorUnknown) {
      describeUnknownEditorMemberAdditions(change, updates);

      describeUnknownEditorModifyMemberRoles(change, updates);
      describeUnknownEditorInvitations(change, updates);
      describeUnknownEditorRevokedInvitations(change, updates);
      describeUnknownEditorPromotePending(change, updates);
      describeUnknownEditorNewTitle(change, updates);
      describeUnknownEditorNewDescription(change, updates);
      describeUnknownEditorNewAvatar(change, updates);
      describeUnknownEditorNewTimer(change, updates);
      describeUnknownEditorNewAttributeAccess(change, updates);
      describeUnknownEditorNewMembershipAccess(change, updates);
      describeUnknownEditorNewGroupInviteLinkAccess(previousGroupState, change, updates);
      describeRequestingMembers(change, updates);
      describeUnknownEditorRequestingMembersApprovals(change, updates);
      describeUnknownEditorRequestingMembersDeletes(change, updates);
      describeUnknownEditorAnnouncementGroupChange(change, updates);
      describeUnknownEditorPromotePendingPniAci(change, updates);

      describeUnknownEditorMemberRemovals(change, updates);

      if (updates.isEmpty()) {
        describeUnknownEditorUnknownChange(updates);
      }

    } else {
      describeMemberAdditions(change, updates);

      describeModifyMemberRoles(change, updates);
      describeInvitations(change, updates);
      describeRevokedInvitations(change, updates);
      describePromotePending(change, updates);
      describeNewTitle(change, updates);
      describeNewDescription(change, updates);
      describeNewAvatar(change, updates);
      describeNewTimer(change, updates);
      describeNewAttributeAccess(change, updates);
      describeNewMembershipAccess(change, updates);
      describeNewGroupInviteLinkAccess(previousGroupState, change, updates);
      describeRequestingMembers(change, updates);
      describeRequestingMembersApprovals(change, updates);
      describeRequestingMembersDeletes(change, updates);
      describeAnnouncementGroupChange(change, updates);
      describePromotePendingPniAci(change, updates);

      describeMemberRemovals(change, updates);

      if (updates.isEmpty()) {
        describeUnknownChange(change, updates);
      }
    }

    return updates;
  }

  /**
   * Handles case of future protocol versions where we don't know what has changed.
   */
  private void describeUnknownChange(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (editorIsYou) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_you_updated_group), Glyph.GROUP));
    } else {
      updates.add(updateDescription(R.string.MessageRecord_s_updated_group, change.editorServiceIdBytes, Glyph.GROUP));
    }
  }

  private void describeUnknownEditorUnknownChange(@NonNull List<UpdateDescription> updates) {
    updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_was_updated), Glyph.GROUP));
  }

  private void describeMemberAdditions(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (DecryptedMember member : change.newMembers) {
      boolean newMemberIsYou = selfIds.matches(member.aciBytes);

      if (editorIsYou) {
        if (newMemberIsYou) {
          updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group_via_the_group_link), Glyph.PERSON_CHECK));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_added_s, member.aciBytes, Glyph.PERSON_PLUS));
        }
      } else {
        if (newMemberIsYou) {
          updates.add(0, updateDescription(R.string.MessageRecord_s_added_you, change.editorServiceIdBytes, Glyph.PERSON_PLUS));
        } else {
          if (member.aciBytes.equals(change.editorServiceIdBytes)) {
            updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group_via_the_group_link, member.aciBytes, Glyph.PERSON_CHECK));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_added_s, change.editorServiceIdBytes, member.aciBytes, Glyph.PERSON_PLUS));
          }
        }
      }
    }
  }

  private void describeUnknownEditorMemberAdditions(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedMember member : change.newMembers) {
      boolean newMemberIsYou = selfIds.matches(member.aciBytes);

      if (newMemberIsYou) {
        updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group, member.aciBytes, Glyph.PERSON_PLUS));
      }
    }
  }

  private void describeMemberRemovals(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (ByteString member : change.deleteMembers) {
      boolean removedMemberIsYou = selfIds.matches(member);

      if (editorIsYou) {
        if (removedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_left_the_group), Glyph.LEAVE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_removed_s, member, Glyph.PERSON_MINUS));
        }
      } else {
        if (removedMemberIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_s_removed_you_from_the_group, change.editorServiceIdBytes, Glyph.PERSON_MINUS));
        } else {
          if (member.equals(change.editorServiceIdBytes)) {
            updates.add(updateDescription(R.string.MessageRecord_s_left_the_group, member, Glyph.LEAVE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_removed_s, change.editorServiceIdBytes, member, Glyph.PERSON_MINUS));
          }
        }
      }
    }
  }

  private void describeUnknownEditorMemberRemovals(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (ByteString member : change.deleteMembers) {
      boolean removedMemberIsYou = selfIds.matches(member);

      if (removedMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_no_longer_in_the_group), Glyph.LEAVE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_is_no_longer_in_the_group, member, Glyph.LEAVE));
      }
    }
  }

  private void describeModifyMemberRoles(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (DecryptedModifyMemberRole roleChange : change.modifyMemberRoles) {
      boolean changedMemberIsYou = selfIds.matches(roleChange.aciBytes);
      if (roleChange.role == Member.Role.ADMINISTRATOR) {
        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_made_s_an_admin, roleChange.aciBytes, Glyph.MEGAPHONE));
        } else {
          if (changedMemberIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_s_made_you_an_admin, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_made_s_an_admin, change.editorServiceIdBytes, roleChange.aciBytes, Glyph.MEGAPHONE));

          }
        }
      } else {
        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_revoked_admin_privileges_from_s, roleChange.aciBytes, Glyph.MEGAPHONE));
        } else {
          if (changedMemberIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_s_revoked_your_admin_privileges, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_revoked_admin_privileges_from_s, change.editorServiceIdBytes, roleChange.aciBytes, Glyph.MEGAPHONE));
          }
        }
      }
    }
  }

  private void describeUnknownEditorModifyMemberRoles(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedModifyMemberRole roleChange : change.modifyMemberRoles) {
      boolean changedMemberIsYou = selfIds.matches(roleChange.aciBytes);

      if (roleChange.role == Member.Role.ADMINISTRATOR) {
        if (changedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_now_an_admin), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_is_now_an_admin, roleChange.aciBytes, Glyph.MEGAPHONE));
        }
      } else {
        if (changedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_no_longer_an_admin), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_is_no_longer_an_admin, roleChange.aciBytes, Glyph.MEGAPHONE));
        }
      }
    }
  }

  private void describeAdminStatusChange(@NonNull GroupAdminStatusUpdate groupAdminStatusUpdate, List<UpdateDescription> updates) {
    boolean changedMemberIsYou = selfIds.matches(groupAdminStatusUpdate.memberAci);

    if (groupAdminStatusUpdate.updaterAci != null) {
      boolean editorIsYou = selfIds.matches(groupAdminStatusUpdate.updaterAci);

      if (groupAdminStatusUpdate.wasAdminStatusGranted) {
        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_made_s_an_admin, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));
        } else {
          if (changedMemberIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_s_made_you_an_admin, groupAdminStatusUpdate.updaterAci, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_made_s_an_admin, groupAdminStatusUpdate.updaterAci, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));

          }
        }
      } else {
        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_revoked_admin_privileges_from_s, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));
        } else {
          if (changedMemberIsYou) {
            updates.add(updateDescription(R.string.MessageRecord_s_revoked_your_admin_privileges, groupAdminStatusUpdate.updaterAci, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_revoked_admin_privileges_from_s, groupAdminStatusUpdate.updaterAci, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));
          }
        }
      }
    } else {
      if (groupAdminStatusUpdate.wasAdminStatusGranted) {
        if (changedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_now_an_admin), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_is_now_an_admin, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));
        }
      } else {
        if (changedMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_are_no_longer_an_admin), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_is_no_longer_an_admin, groupAdminStatusUpdate.memberAci, Glyph.MEGAPHONE));
        }
      }
    }
  }

  private void describeInvitations(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou       = selfIds.matches(change.editorServiceIdBytes);
    int     notYouInviteCount = 0;

    for (DecryptedPendingMember invitee : change.newPendingMembers) {
      boolean newMemberIsYou = selfIds.matches(invitee.serviceIdBytes);

      if (newMemberIsYou) {
        updates.add(0, updateDescription(R.string.MessageRecord_s_invited_you_to_the_group, change.editorServiceIdBytes, Glyph.PERSON_PLUS));
      } else {
        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_invited_s_to_the_group, invitee.serviceIdBytes, Glyph.PERSON_PLUS));
        } else {
          notYouInviteCount++;
        }
      }
    }

    if (notYouInviteCount > 0) {
      updates.add(updateDescription(R.plurals.MessageRecord_s_invited_members, notYouInviteCount, change.editorServiceIdBytes, notYouInviteCount, Glyph.PERSON_PLUS));
    }
  }

  private void describeUnknownEditorInvitations(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    int notYouInviteCount = 0;

    for (DecryptedPendingMember invitee : change.newPendingMembers) {
      boolean newMemberIsYou = selfIds.matches(invitee.serviceIdBytes);

      if (newMemberIsYou) {
        UUID uuid = UuidUtil.fromByteStringOrUnknown(invitee.addedByAci);

        if (UuidUtil.UNKNOWN_UUID.equals(uuid)) {
          updates.add(0, updateDescription(context.getString(R.string.MessageRecord_you_were_invited_to_the_group), Glyph.PERSON_PLUS));
        } else {
          updates.add(0, updateDescription(R.string.MessageRecord_s_invited_you_to_the_group, invitee.addedByAci, Glyph.PERSON_PLUS));
        }
      } else {
        notYouInviteCount++;
      }
    }

    if (notYouInviteCount > 0) {
      updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_d_people_were_invited_to_the_group, notYouInviteCount, notYouInviteCount), Glyph.PERSON_PLUS));
    }
  }

  private void describeRevokedInvitations(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou     = selfIds.matches(change.editorServiceIdBytes);
    int     notDeclineCount = 0;

    for (DecryptedPendingMemberRemoval invitee : change.deletePendingMembers) {
      boolean decline = invitee.serviceIdBytes.equals(change.editorServiceIdBytes);
      if (decline) {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_declined_the_invitation_to_the_group), Glyph.PERSON_X));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_someone_declined_an_invitation_to_the_group), Glyph.PERSON_X));
        }
      } else if (selfIds.matches(invitee.serviceIdBytes)) {
        updates.add(updateDescription(R.string.MessageRecord_s_revoked_your_invitation_to_the_group, change.editorServiceIdBytes, Glyph.PERSON_X));
      } else {
        notDeclineCount++;
      }
    }

    if (notDeclineCount > 0) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_you_revoked_invites, notDeclineCount, notDeclineCount), Glyph.PERSON_X));
      } else {
        updates.add(updateDescription(R.plurals.MessageRecord_s_revoked_invites, notDeclineCount, change.editorServiceIdBytes, notDeclineCount, Glyph.PERSON_X));
      }
    }
  }

  private void describeUnknownEditorRevokedInvitations(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    int notDeclineCount = 0;

    for (DecryptedPendingMemberRemoval invitee : change.deletePendingMembers) {
      boolean inviteeWasYou = selfIds.matches(invitee.serviceIdBytes);

      if (inviteeWasYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_an_admin_revoked_your_invitation_to_the_group), Glyph.PERSON_X));
      } else {
        notDeclineCount++;
      }
    }

    if (notDeclineCount > 0) {
      updates.add(updateDescription(context.getResources().getQuantityString(R.plurals.MessageRecord_d_invitations_were_revoked, notDeclineCount, notDeclineCount), Glyph.PERSON_X));
    }
  }

  private void describePromotePending(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (DecryptedMember newMember : change.promotePendingMembers) {
      ByteString aci            = newMember.aciBytes;
      boolean    newMemberIsYou = selfIds.matches(aci);

      if (editorIsYou) {
        if (newMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_accepted_invite), Glyph.PERSON_CHECK));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_added_invited_member_s, aci, Glyph.PERSON_PLUS));
        }
      } else {
        if (newMemberIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_s_added_you, change.editorServiceIdBytes, Glyph.PERSON_PLUS));
        } else {
          if (aci.equals(change.editorServiceIdBytes)) {
            updates.add(updateDescription(R.string.MessageRecord_s_accepted_invite, aci, Glyph.PERSON_CHECK));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_added_invited_member_s, change.editorServiceIdBytes, aci, Glyph.PERSON_PLUS));
          }
        }
      }
    }
  }

  private void describeUnknownEditorPromotePending(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedMember newMember : change.promotePendingMembers) {
      ByteString aci            = newMember.aciBytes;
      boolean    newMemberIsYou = selfIds.matches(aci);

      if (newMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group, aci, Glyph.PERSON_PLUS));
      }
    }
  }

  private void describeNewTitle(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newTitle != null) {
      String newTitle = BidiUtil.isolateBidi(change.newTitle.value_);
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_name_to_s, newTitle), Glyph.EDIT));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_name_to_s, change.editorServiceIdBytes, newTitle, Glyph.EDIT));
      }
    }
  }

  private void describeNewDescription(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newDescription != null) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_description), Glyph.EDIT));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_description, change.editorServiceIdBytes, Glyph.EDIT));
      }
    }
  }

  private void describeUnknownEditorNewTitle(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newTitle != null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_name_has_changed_to_s, BidiUtil.isolateBidi(change.newTitle.value_)), Glyph.EDIT));
    }
  }

  private void describeDescriptionChange(@NonNull GroupDescriptionUpdate groupDescriptionUpdate, @NonNull List<UpdateDescription> updates) {
    if (groupDescriptionUpdate.updaterAci != null) {
      boolean editorIsYou = selfIds.matches(groupDescriptionUpdate.updaterAci);

      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_description), Glyph.EDIT));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_description, groupDescriptionUpdate.updaterAci, Glyph.EDIT));
      }
    } else {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_name_has_changed_to_s, BidiUtil.isolateBidi(groupDescriptionUpdate.newDescription)), Glyph.EDIT));
    }
  }

  private void describeUnknownEditorNewDescription(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newDescription != null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_description_has_changed), Glyph.EDIT));
    }
  }

  private void describeNewAvatar(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newAvatar != null) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_avatar), Glyph.PHOTO));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_avatar, change.editorServiceIdBytes, Glyph.PHOTO));
      }
    }
  }

  private void describeAvatarChange(@NonNull GroupAvatarUpdate groupAvatarUpdate, @NonNull List<UpdateDescription> updates) {
    if (groupAvatarUpdate.updaterAci != null) {
      boolean editorIsYou = selfIds.matches(groupAvatarUpdate.updaterAci);

      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_the_group_avatar), Glyph.PHOTO));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_the_group_avatar, groupAvatarUpdate.updaterAci, Glyph.PHOTO));
      }
    } else {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_group_avatar_has_been_changed), Glyph.PHOTO));
    }
  }

  private void describeUnknownEditorNewAvatar(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newAvatar != null) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_group_avatar_has_been_changed), Glyph.PHOTO));
    }
  }

  void describeNewTimer(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newTimer != null) {
      final int duration = change.newTimer.duration;
      if (duration <= 0) {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_disabled_disappearing_messages), Glyph.TIMER_SLASH));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_disabled_disappearing_messages, change.editorServiceIdBytes, Glyph.TIMER_SLASH));
        }
      } else {
        String time = ExpirationUtil.getExpirationDisplayValue(context, duration);
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_set_disappearing_message_time_to_s, time), Glyph.TIMER));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_set_disappearing_message_time_to_s, change.editorServiceIdBytes, time, Glyph.TIMER));
        }
      }
    }
  }

  private void describeUnknownEditorNewTimer(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newTimer != null) {
      String time = ExpirationUtil.getExpirationDisplayValue(context, change.newTimer.duration);
      updates.add(updateDescription(context.getString(R.string.MessageRecord_disappearing_message_time_set_to_s, time), Glyph.TIMER));
    }
  }

  private void describeNewAttributeAccess(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newAttributeAccess != AccessControl.AccessRequired.UNKNOWN) {
      String accessLevel = GV2AccessLevelUtil.toString(context, change.newAttributeAccess);
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_who_can_edit_group_info_to_s, accessLevel), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_who_can_edit_group_info_to_s, change.editorServiceIdBytes, accessLevel, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeUnknownEditorNewAttributeAccess(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newAttributeAccess != AccessControl.AccessRequired.UNKNOWN) {
      String accessLevel = GV2AccessLevelUtil.toString(context, change.newAttributeAccess);
      updates.add(updateDescription(context.getString(R.string.MessageRecord_who_can_edit_group_info_has_been_changed_to_s, accessLevel), Glyph.MEGAPHONE));
    }
  }

  private void describeNewMembershipAccess(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newMemberAccess != AccessControl.AccessRequired.UNKNOWN) {
      String accessLevel = GV2AccessLevelUtil.toString(context, change.newMemberAccess);
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_changed_who_can_edit_group_membership_to_s, accessLevel), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_changed_who_can_edit_group_membership_to_s, change.editorServiceIdBytes, accessLevel, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeUnknownEditorNewMembershipAccess(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newMemberAccess != AccessControl.AccessRequired.UNKNOWN) {
      String accessLevel = GV2AccessLevelUtil.toString(context, change.newMemberAccess);
      updates.add(updateDescription(context.getString(R.string.MessageRecord_who_can_edit_group_membership_has_been_changed_to_s, accessLevel), Glyph.MEGAPHONE));
    }
  }

  private void describeNewGroupInviteLinkAccess(@Nullable DecryptedGroup previousGroupState,
                                                @NonNull DecryptedGroupChange change,
                                                @NonNull List<UpdateDescription> updates)
  {
    AccessControl.AccessRequired previousAccessControl = null;

    if (previousGroupState != null && previousGroupState.accessControl != null) {
      previousAccessControl = previousGroupState.accessControl.addFromInviteLink;
    }

    boolean editorIsYou      = selfIds.matches(change.editorServiceIdBytes);
    boolean groupLinkEnabled = false;

    //noinspection EnhancedSwitchMigration
    switch (change.newInviteLinkAccess) {
      case ANY:
        groupLinkEnabled = true;
        if (editorIsYou) {
          if (previousAccessControl == AccessControl.AccessRequired.ADMINISTRATOR) {
            updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_off_admin_approval_for_the_group_link), Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_the_group_link_with_admin_approval_off), Glyph.MEGAPHONE));
          }
        } else {
          if (previousAccessControl == AccessControl.AccessRequired.ADMINISTRATOR) {
            updates.add(updateDescription(R.string.MessageRecord_s_turned_off_admin_approval_for_the_group_link, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_turned_on_the_group_link_with_admin_approval_off, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          }
        }
        break;
      case ADMINISTRATOR:
        groupLinkEnabled = true;
        if (editorIsYou) {
          if (previousAccessControl == AccessControl.AccessRequired.ANY) {
            updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_admin_approval_for_the_group_link), Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_on_the_group_link_with_admin_approval_on), Glyph.MEGAPHONE));
          }
        } else {
          if (previousAccessControl == AccessControl.AccessRequired.ANY) {
            updates.add(updateDescription(R.string.MessageRecord_s_turned_on_admin_approval_for_the_group_link, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_turned_on_the_group_link_with_admin_approval_on, change.editorServiceIdBytes, Glyph.MEGAPHONE));
          }
        }
        break;
      case UNSATISFIABLE:
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_turned_off_the_group_link), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_turned_off_the_group_link, change.editorServiceIdBytes, Glyph.MEGAPHONE));
        }
        break;
    }

    if (!groupLinkEnabled && change.newInviteLinkPassword.size() > 0) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_reset_the_group_link), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_reset_the_group_link, change.editorServiceIdBytes, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeUnknownEditorNewGroupInviteLinkAccess(@Nullable DecryptedGroup previousGroupState,
                                                             @NonNull DecryptedGroupChange change,
                                                             @NonNull List<UpdateDescription> updates)
  {
    AccessControl.AccessRequired previousAccessControl = null;

    if (previousGroupState != null && previousGroupState.accessControl != null) {
      previousAccessControl = previousGroupState.accessControl.addFromInviteLink;
    }

    //noinspection EnhancedSwitchMigration
    switch (change.newInviteLinkAccess) {
      case ANY:
        if (previousAccessControl == AccessControl.AccessRequired.ADMINISTRATOR) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_the_admin_approval_for_the_group_link_has_been_turned_off), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_on_with_admin_approval_off), Glyph.MEGAPHONE));
        }
        break;
      case ADMINISTRATOR:
        if (previousAccessControl == AccessControl.AccessRequired.ANY) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_the_admin_approval_for_the_group_link_has_been_turned_on), Glyph.MEGAPHONE));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_on_with_admin_approval_on), Glyph.MEGAPHONE));
        }
        break;
      case UNSATISFIABLE:
        updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_turned_off), Glyph.MEGAPHONE));
        break;
    }

    if (change.newInviteLinkPassword.size() > 0) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_the_group_link_has_been_reset), Glyph.MEGAPHONE));
    }
  }

  private void describeRequestingMembers(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    Set<ByteString> deleteRequestingUuids = new HashSet<>(change.deleteRequestingMembers);

    for (DecryptedRequestingMember member : change.newRequestingMembers) {
      boolean requestingMemberIsYou = selfIds.matches(member.aciBytes);

      if (requestingMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_sent_a_request_to_join_the_group), Glyph.GROUP));
      } else {
        if (deleteRequestingUuids.contains(member.aciBytes)) {
          updates.add(updateDescription(R.plurals.MessageRecord_s_requested_and_cancelled_their_request_to_join_via_the_group_link,
                                        change.deleteRequestingMembers.size(),
                                        member.aciBytes,
                                        change.deleteRequestingMembers.size(),
                                        Glyph.GROUP));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_requested_to_join_via_the_group_link, member.aciBytes, Glyph.GROUP));
        }
      }
    }
  }

  private void describeRequestingMembersApprovals(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedApproveMember requestingMember : change.promoteRequestingMembers) {
      boolean requestingMemberIsYou = selfIds.matches(requestingMember.aciBytes);

      if (requestingMemberIsYou) {
        updates.add(updateDescription(R.string.MessageRecord_s_approved_your_request_to_join_the_group, change.editorServiceIdBytes, Glyph.PERSON_CHECK));
      } else {
        boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

        if (editorIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_you_approved_a_request_to_join_the_group_from_s, requestingMember.aciBytes, Glyph.PERSON_CHECK));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_approved_a_request_to_join_the_group_from_s, change.editorServiceIdBytes, requestingMember.aciBytes, Glyph.PERSON_CHECK));
        }
      }
    }
  }

  private void describeUnknownEditorRequestingMembersApprovals(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedApproveMember requestingMember : change.promoteRequestingMembers) {
      boolean requestingMemberIsYou = selfIds.matches(requestingMember.aciBytes);

      if (requestingMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_approved), Glyph.PERSON_CHECK));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_a_request_to_join_the_group_from_s_has_been_approved, requestingMember.aciBytes, Glyph.PERSON_CHECK));
      }
    }
  }

  private void describeRequestingMembersDeletes(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    Set<ByteString> newRequestingUuids = change.newRequestingMembers.stream().map(m -> m.aciBytes).collect(Collectors.toSet());

    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (ByteString requestingMember : change.deleteRequestingMembers) {
      if (newRequestingUuids.contains(requestingMember)) {
        continue;
      }

      boolean requestingMemberIsYou = selfIds.matches(requestingMember);

      if (requestingMemberIsYou) {
        if (editorIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_canceled_your_request_to_join_the_group), Glyph.PERSON_X));
        } else {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_denied_by_an_admin), Glyph.PERSON_X));
        }
      } else {
        boolean editorIsCanceledMember = change.editorServiceIdBytes.equals(requestingMember);

        if (editorIsCanceledMember) {
          updates.add(updateDescription(R.string.MessageRecord_s_canceled_their_request_to_join_the_group, requestingMember, Glyph.PERSON_X));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_s_denied_a_request_to_join_the_group_from_s, change.editorServiceIdBytes, requestingMember, Glyph.PERSON_X));
        }
      }
    }
  }

  private void describeUnknownEditorRequestingMembersDeletes(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (ByteString requestingMember : change.deleteRequestingMembers) {
      boolean requestingMemberIsYou = selfIds.matches(requestingMember);

      if (requestingMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_your_request_to_join_the_group_has_been_denied_by_an_admin), Glyph.PERSON_X));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_a_request_to_join_the_group_from_s_has_been_denied, requestingMember, Glyph.PERSON_X));
      }
    }
  }

  private void describeAnnouncementGroupChange(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    if (change.newIsAnnouncementGroup == EnabledState.ENABLED) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_allow_only_admins_to_send), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_allow_only_admins_to_send, change.editorServiceIdBytes, Glyph.MEGAPHONE));
      }
    } else if (change.newIsAnnouncementGroup == EnabledState.DISABLED) {
      if (editorIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_allow_all_members_to_send), Glyph.MEGAPHONE));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_allow_all_members_to_send, change.editorServiceIdBytes, Glyph.MEGAPHONE));
      }
    }
  }

  private void describeUnknownEditorAnnouncementGroupChange(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    if (change.newIsAnnouncementGroup == EnabledState.ENABLED) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_allow_only_admins_to_send), Glyph.MEGAPHONE));
    } else if (change.newIsAnnouncementGroup == EnabledState.DISABLED) {
      updates.add(updateDescription(context.getString(R.string.MessageRecord_allow_all_members_to_send), Glyph.MEGAPHONE));
    }
  }

  private void describePromotePendingPniAci(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    boolean editorIsYou = selfIds.matches(change.editorServiceIdBytes);

    for (DecryptedMember newMember : change.promotePendingPniAciMembers) {
      ByteString uuid           = newMember.aciBytes;
      boolean    newMemberIsYou = selfIds.matches(uuid);

      if (editorIsYou) {
        if (newMemberIsYou) {
          updates.add(updateDescription(context.getString(R.string.MessageRecord_you_accepted_invite), Glyph.PERSON_CHECK));
        } else {
          updates.add(updateDescription(R.string.MessageRecord_you_added_invited_member_s, uuid, Glyph.PERSON_PLUS));
        }
      } else {
        if (newMemberIsYou) {
          updates.add(updateDescription(R.string.MessageRecord_s_added_you, change.editorServiceIdBytes, Glyph.PERSON_PLUS));
        } else {
          if (uuid.equals(change.editorServiceIdBytes)) {
            updates.add(updateDescription(R.string.MessageRecord_s_accepted_invite, uuid, Glyph.PERSON_CHECK));
          } else {
            updates.add(updateDescription(R.string.MessageRecord_s_added_invited_member_s, change.editorServiceIdBytes, uuid, Glyph.PERSON_PLUS));
          }
        }
      }
    }
  }

  private void describeUnknownEditorPromotePendingPniAci(@NonNull DecryptedGroupChange change, @NonNull List<UpdateDescription> updates) {
    for (DecryptedMember newMember : change.promotePendingPniAciMembers) {
      ByteString aci            = newMember.aciBytes;
      boolean    newMemberIsYou = selfIds.matches(aci);

      if (newMemberIsYou) {
        updates.add(updateDescription(context.getString(R.string.MessageRecord_you_joined_the_group), Glyph.PERSON_PLUS));
      } else {
        updates.add(updateDescription(R.string.MessageRecord_s_joined_the_group, aci, Glyph.PERSON_PLUS));
      }
    }
  }

  private static UpdateDescription updateDescription(@NonNull String string, Glyph glyph) {
    return UpdateDescription.staticDescription(string, glyph);
  }

  private UpdateDescription updateDescription(@StringRes int stringRes,
                                              @NonNull ByteString serviceId1Bytes,
                                              Glyph glyph)
  {
    ServiceId   serviceId   = ServiceId.parseOrUnknown(serviceId1Bytes);
    RecipientId recipientId = RecipientId.from(serviceId);

    return UpdateDescription.mentioning(
        Collections.singletonList(serviceId),
        () -> {
          List<RecipientId> recipientIdList = Collections.singletonList(recipientId);
          String            templateString  = context.getString(stringRes, makePlaceholders(recipientIdList, null));

          return makeRecipientsClickable(context, templateString, recipientIdList, recipientClickHandler);
        },
        glyph);
  }

  private UpdateDescription updateDescription(@StringRes int stringRes,
                                              @NonNull ByteString serviceId1Bytes,
                                              @NonNull ByteString serviceId2Bytes,
                                              Glyph glyph)
  {
    ServiceId serviceId1 = ServiceId.parseOrUnknown(serviceId1Bytes);
    ServiceId serviceId2 = ServiceId.parseOrUnknown(serviceId2Bytes);

    RecipientId recipientId1 = RecipientId.from(serviceId1);
    RecipientId recipientId2 = RecipientId.from(serviceId2);

    return UpdateDescription.mentioning(
        Arrays.asList(serviceId1, serviceId2),
        () -> {
          List<RecipientId> recipientIdList = Arrays.asList(recipientId1, recipientId2);
          String            templateString  = context.getString(stringRes, makePlaceholders(recipientIdList, null));

          return makeRecipientsClickable(context, templateString, recipientIdList, recipientClickHandler);
        },
        glyph
    );
  }

  private UpdateDescription updateDescription(@StringRes int stringRes,
                                              @NonNull ByteString serviceId1Bytes,
                                              @NonNull Object formatArg,
                                              Glyph glyph)
  {
    ServiceId   serviceId   = ServiceId.parseOrUnknown(serviceId1Bytes);
    RecipientId recipientId = RecipientId.from(serviceId);

    return UpdateDescription.mentioning(
        Collections.singletonList(serviceId),
        () -> {
          List<RecipientId> recipientIdList = Collections.singletonList(recipientId);
          String            templateString  = context.getString(stringRes, makePlaceholders(recipientIdList, Collections.singletonList(formatArg)));

          return makeRecipientsClickable(context, templateString, recipientIdList, recipientClickHandler);
        },
        glyph
    );
  }

  private UpdateDescription updateDescription(@PluralsRes int stringRes,
                                              int quantity,
                                              @NonNull ByteString serviceId1Bytes,
                                              @NonNull Object formatArg,
                                              Glyph glyph)
  {
    ServiceId   serviceId   = ServiceId.parseOrUnknown(serviceId1Bytes);
    RecipientId recipientId = RecipientId.from(serviceId);

    return UpdateDescription.mentioning(
        Collections.singletonList(serviceId),
        () -> {
          List<RecipientId> recipientIdList = Collections.singletonList(recipientId);
          String            templateString  = context.getResources().getQuantityString(stringRes, quantity, makePlaceholders(recipientIdList, Collections.singletonList(formatArg)));

          return makeRecipientsClickable(context, templateString, recipientIdList, recipientClickHandler);
        },
        glyph
    );
  }

  private static @NonNull Object[] makePlaceholders(@NonNull List<RecipientId> recipientIds, @Nullable List<Object> formatArgs) {
    List<Object> args = recipientIds.stream().map(GroupsV2UpdateMessageProducer::makePlaceholder).collect(Collectors.toList());

    if (formatArgs != null) {
      args.addAll(formatArgs);
    }

    return args.toArray();
  }

  @VisibleForTesting
  static @NonNull Spannable makeRecipientsClickable(@NonNull Context context, @NonNull String template, @NonNull List<RecipientId> recipientIds, @Nullable Consumer<RecipientId> clickHandler) {
    SpannableStringBuilder builder    = new SpannableStringBuilder();
    int                    startIndex = 0;

    Map<String, RecipientId> idByPlaceholder = new HashMap<>();
    for (RecipientId id : recipientIds) {
      idByPlaceholder.put(makePlaceholder(id), id);
    }

    while (startIndex < template.length()) {
      Map.Entry<String, RecipientId> nearestEntry    = null;
      int                            nearestPosition = Integer.MAX_VALUE;

      for (Map.Entry<String, RecipientId> entry : idByPlaceholder.entrySet()) {
        String placeholder      = entry.getKey();
        int    placeholderStart = template.indexOf(placeholder, startIndex);

        if (placeholderStart >= 0 && placeholderStart < nearestPosition) {
          nearestPosition = placeholderStart;
          nearestEntry    = entry;
        }
      }

      if (nearestEntry != null) {
        String      placeholder = nearestEntry.getKey();
        RecipientId recipientId = nearestEntry.getValue();

        String beforeChunk = template.substring(startIndex, nearestPosition);

        builder.append(beforeChunk);
        builder.append(SpanUtil.clickable(Recipient.resolved(recipientId).getDisplayName(context), ContextCompat.getColor(context, R.color.conversation_item_update_text_color), v -> {
          if (!recipientId.isUnknown() && clickHandler != null) {
            clickHandler.accept(recipientId);
          }
        }));

        startIndex = nearestPosition + placeholder.length();
      } else {
        builder.append(template.substring(startIndex));
        startIndex = template.length();
      }
    }

    return builder;
  }

  @VisibleForTesting
  static @NonNull String makePlaceholder(@NonNull RecipientId recipientId) {
    return "{{SPAN_PLACEHOLDER_" + recipientId + "}}";
  }
}