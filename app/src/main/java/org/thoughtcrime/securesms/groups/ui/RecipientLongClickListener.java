package org.thoughtcrime.securesms.ryan.groups.ui;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.recipients.Recipient;

public interface RecipientLongClickListener {
  boolean onLongClick(@NonNull Recipient recipient);
}
