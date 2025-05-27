package org.thoughtcrime.securesms.ryan.groups.ui;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.recipients.Recipient;

public interface RecipientClickListener {
  void onClick(@NonNull Recipient recipient);
}
