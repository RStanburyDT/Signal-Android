package org.thoughtcrime.securesms.ryan.storage;

import androidx.annotation.NonNull;

/**
 * Generates a key for use with the storage service.
 */
public interface StorageKeyGenerator {
  @NonNull
  byte[] generate();
}
