package org.thoughtcrime.securesms.ryan.sharing.interstitial;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter;
import org.thoughtcrime.securesms.ryan.util.viewholders.RecipientViewHolder;

class ShareInterstitialSelectionAdapter extends MappingAdapter {
  ShareInterstitialSelectionAdapter() {
    registerFactory(ShareInterstitialMappingModel.class, RecipientViewHolder.createFactory(R.layout.share_contact_selection_item, null));
  }
}
