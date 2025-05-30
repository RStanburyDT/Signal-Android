package org.thoughtcrime.securesms.ryan.glide.cache;


import androidx.annotation.NonNull;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteBufferUtil;

import org.signal.core.util.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class EncryptedGifDrawableResourceEncoder extends EncryptedCoder implements ResourceEncoder<GifDrawable> {

  private static final String TAG = Log.tag(EncryptedGifDrawableResourceEncoder.class);

  private final byte[] secret;

  public EncryptedGifDrawableResourceEncoder(@NonNull byte[] secret) {
    this.secret = secret;
  }

  @Override
  public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
    return EncodeStrategy.TRANSFORMED;
  }

  @Override
  public boolean encode(@NonNull Resource<GifDrawable> data, @NonNull File file, @NonNull Options options) {
    GifDrawable drawable = data.get();

    try (OutputStream outputStream = createEncryptedOutputStream(secret, file)) {
      ByteBufferUtil.toStream(drawable.getBuffer(), outputStream);
      return true;
    } catch (IOException e) {
      Log.w(TAG, e);
      return false;
    }
  }
}
