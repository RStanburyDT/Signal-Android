package org.thoughtcrime.securesms.ryan.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K,V> extends LinkedHashMap<K,V> {

  private final int maxSize;

  public LRUCache(int maxSize) {
    super(maxSize / 2, 0.75f, true);
    this.maxSize = maxSize;
  }

  @Override
  protected boolean removeEldestEntry (Map.Entry<K,V> eldest) {
    return size() > maxSize;
  }
}
