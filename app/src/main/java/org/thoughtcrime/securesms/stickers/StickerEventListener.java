package org.thoughtcrime.securesms.ryan.stickers;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.database.model.StickerRecord;

public interface StickerEventListener {
  void onStickerSelected(@NonNull StickerRecord sticker);

  void onStickerManagementClicked();
}
