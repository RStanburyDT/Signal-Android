package org.thoughtcrime.securesms.ryan.util.adapter.mapping;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

public interface Factory<T extends MappingModel<T>> {
  @NonNull MappingViewHolder<T> createViewHolder(@NonNull ViewGroup parent);
}
