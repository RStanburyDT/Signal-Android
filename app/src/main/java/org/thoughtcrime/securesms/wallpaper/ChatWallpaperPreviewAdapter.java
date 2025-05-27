package org.thoughtcrime.securesms.ryan.wallpaper;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter;

class ChatWallpaperPreviewAdapter extends MappingAdapter {
  ChatWallpaperPreviewAdapter() {
    registerFactory(ChatWallpaperSelectionMappingModel.class, ChatWallpaperViewHolder.createFactory(R.layout.chat_wallpaper_preview_fragment_adapter_item, null, null));
  }
}
