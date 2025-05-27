package org.thoughtcrime.securesms.ryan.components.settings;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.ryan.R;
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.LayoutFactory;
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter;

/**
 * Reusable adapter for generic settings list.
 */
public class BaseSettingsAdapter extends MappingAdapter {

  public BaseSettingsAdapter() {
    registerFactory(SettingHeader.Item.class, SettingHeader.ViewHolder::new, R.layout.base_settings_header_item);
    registerFactory(SettingProgress.Item.class, SettingProgress.ViewHolder::new, R.layout.base_settings_progress_item);
  }

  public void configureSingleSelect(@NonNull SingleSelectSetting.SingleSelectSelectionChangedListener selectionChangedListener) {
    registerFactory(SingleSelectSetting.Item.class,
                    new LayoutFactory<>(v -> new SingleSelectSetting.ViewHolder(v, selectionChangedListener), R.layout.single_select_item));
  }
}
