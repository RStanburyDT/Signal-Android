package org.thoughtcrime.securesms.ryan.database.documents;

import java.util.Set;

public interface Document<T> {
  int size();
  Set<T> getItems();
}
