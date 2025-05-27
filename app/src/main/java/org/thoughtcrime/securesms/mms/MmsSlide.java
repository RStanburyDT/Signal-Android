package org.thoughtcrime.securesms.ryan.mms;


import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.attachments.Attachment;

public class MmsSlide extends ImageSlide {

  public MmsSlide(@NonNull Attachment attachment) {
    super(attachment);
  }

  @NonNull
  @Override
  public String getContentDescription(Context context) {
    return "MMS";
  }

}
