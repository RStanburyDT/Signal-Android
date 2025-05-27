package org.thoughtcrime.securesms.ryan.payments.preferences.model;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel;

public class InProgress implements MappingModel<InProgress> {
  @Override
  public boolean areItemsTheSame(@NonNull InProgress newItem) {
    return true;
  }

  @Override
  public boolean areContentsTheSame(@NonNull InProgress newItem) {
    return true;
  }
}
