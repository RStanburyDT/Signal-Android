package org.thoughtcrime.securesms.ryan.sharing;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter;

public class ShareSelectionAdapter extends MappingAdapter {
  public ShareSelectionAdapter() {
    registerFactory(ShareSelectionMappingModel.class,
                    ShareSelectionViewHolder.createFactory(R.layout.share_contact_selection_item));
  }
}
