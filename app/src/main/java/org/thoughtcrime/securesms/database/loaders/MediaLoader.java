package org.thoughtcrime.securesms.ryan.database.loaders;

import android.content.Context;

import org.thoughtcrime.securesms.ryan.util.AbstractCursorLoader;

public abstract class MediaLoader extends AbstractCursorLoader {

  MediaLoader(Context context) {
    super(context);
  }

  public enum MediaType {
    GALLERY,
    DOCUMENT,
    AUDIO,
    ALL
  }
}
