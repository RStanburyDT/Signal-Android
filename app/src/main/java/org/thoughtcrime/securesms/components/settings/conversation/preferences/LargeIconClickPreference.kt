package org.thoughtcrime.securesms.ryan.components.settings.conversation.preferences

import android.view.View
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsIcon
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.ryan.components.settings.PreferenceModel
import org.thoughtcrime.securesms.ryan.components.settings.PreferenceViewHolder
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter

/**
 * Renders a preference line item with a larger (40dp) icon
 */
object LargeIconClickPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.large_icon_preference_item))
  }

  class Model(
    override val title: DSLSettingsText?,
    override val icon: DSLSettingsIcon,
    override val summary: DSLSettingsText? = null,
    override val isEnabled: Boolean = true,
    val onClick: () -> Unit
  ) : PreferenceModel<Model>()

  private class ViewHolder(itemView: View) : PreferenceViewHolder<Model>(itemView) {
    override fun bind(model: Model) {
      super.bind(model)
      itemView.setOnClickListener { model.onClick() }
    }
  }
}
