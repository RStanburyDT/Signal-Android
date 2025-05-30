package org.thoughtcrime.securesms.ryan.mms;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.ryan.attachments.Attachment;
import org.thoughtcrime.securesms.ryan.util.MediaUtil;

public class GifSlide extends ImageSlide {

  private final boolean borderless;

  public GifSlide(Attachment attachment) {
    super(attachment);
    this.borderless = attachment.borderless;
  }

  public GifSlide(Context context, Uri uri, long size, int width, int height) {
    this(context, uri, size, width, height, false, null);
  }

  public GifSlide(Context context, Uri uri, long size, int width, int height, boolean borderless, @Nullable String caption) {
    super(constructAttachmentFromUri(context,
                                     uri,
                                     MediaUtil.IMAGE_GIF,
                                     size,
                                     width,
                                     height,
                                     true,
                                     null,
                                     caption,
                                     null,
                                     null,
                                     null,
                                     false,
                                     borderless,
                                     true,
                                     false));

    this.borderless = borderless;
  }

  @Override
  public boolean isBorderless() {
    return borderless;
  }

  @Override
  public boolean isVideoGif() {
    return true;
  }
}
