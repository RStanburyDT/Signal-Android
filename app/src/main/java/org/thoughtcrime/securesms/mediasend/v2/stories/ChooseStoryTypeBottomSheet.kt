package org.thoughtcrime.securesms.ryan.mediasend.v2.stories

import org.signal.core.util.DimensionUnit
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsAdapter
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsBottomSheetFragment
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsIcon
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.ryan.components.settings.configure
import org.thoughtcrime.securesms.ryan.components.settings.conversation.preferences.LargeIconClickPreference
import org.thoughtcrime.securesms.ryan.util.fragments.requireListener

class ChooseStoryTypeBottomSheet : DSLSettingsBottomSheetFragment(
  layoutId = R.layout.dsl_settings_bottom_sheet
) {
  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    LargeIconClickPreference.register(adapter)
    adapter.submitList(getConfiguration().toMappingModelList())
  }

  private fun getConfiguration(): DSLConfiguration {
    return configure {
      textPref(
        title = DSLSettingsText.from(
          stringId = R.string.ChooseStoryTypeBottomSheet__choose_your_story_type,
          DSLSettingsText.CenterModifier,
          DSLSettingsText.TitleMediumModifier
        )
      )

      customPref(
        LargeIconClickPreference.Model(
          title = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__new_custom_story
          ),
          summary = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__visible_only_to
          ),
          icon = DSLSettingsIcon.from(
            iconId = R.drawable.symbol_stories_24,
            iconTintId = R.color.signal_colorOnSurface,
            backgroundId = R.drawable.circle_tintable,
            backgroundTint = R.color.signal_colorSurface5,
            insetPx = DimensionUnit.DP.toPixels(8f).toInt()
          ),
          onClick = {
            dismissAllowingStateLoss()
            requireListener<Callback>().onNewStoryClicked()
          }
        )
      )

      customPref(
        LargeIconClickPreference.Model(
          title = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__group_story
          ),
          summary = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__share_to_an_existing_group
          ),
          icon = DSLSettingsIcon.from(
            iconId = R.drawable.ic_group_outline_24,
            iconTintId = R.color.signal_colorOnSurface,
            backgroundId = R.drawable.circle_tintable,
            backgroundTint = R.color.signal_colorSurface5,
            insetPx = DimensionUnit.DP.toPixels(8f).toInt()
          ),
          onClick = {
            dismissAllowingStateLoss()
            requireListener<Callback>().onGroupStoryClicked()
          }
        )
      )

      space(DimensionUnit.DP.toPixels(32f).toInt())
    }
  }

  interface Callback {
    fun onNewStoryClicked()
    fun onGroupStoryClicked()
  }
}
