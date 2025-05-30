package org.thoughtcrime.securesms.ryan.conversation.v2.data;

import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.ryan.database.SignalDatabase;
import org.thoughtcrime.securesms.ryan.database.model.MmsMessageRecord;
import org.thoughtcrime.securesms.ryan.database.model.MessageRecord;
import org.thoughtcrime.securesms.ryan.util.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttachmentHelper {

  private Collection<Long>                    messageIds             = new LinkedList<>();
  private Map<Long, List<DatabaseAttachment>> messageIdToAttachments = new HashMap<>();

  public void add(MessageRecord record) {
    if (record.isMms()) {
      messageIds.add(record.getId());
    }
  }

  public void addAll(List<MessageRecord> records) {
    for (MessageRecord record : records) {
      add(record);
    }
  }

  public void fetchAttachments() {
    messageIdToAttachments = SignalDatabase.attachments().getAttachmentsForMessages(messageIds);
  }

  public @NonNull List<MessageRecord> buildUpdatedModels(@NonNull Context context, @NonNull List<MessageRecord> records) {
    return records.stream()
                  .map(record -> {
                    if (record instanceof MmsMessageRecord) {
                      List<DatabaseAttachment> attachments = messageIdToAttachments.get(record.getId());

                      if (Util.hasItems(attachments)) {
                        return ((MmsMessageRecord) record).withAttachments(attachments);
                      }
                    }

                    return record;
                  })
                  .collect(Collectors.toList());
  }
}
