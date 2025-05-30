package org.thoughtcrime.securesms.ryan.stories.viewer.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.ryan.components.settings.configure
import org.thoughtcrime.securesms.ryan.conversation.ConversationIntents
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.stories.settings.StorySettingsActivity
import org.thoughtcrime.securesms.ryan.stories.viewer.reply.StoryViewsAndRepliesPagerChild
import org.thoughtcrime.securesms.ryan.stories.viewer.reply.StoryViewsAndRepliesPagerParent
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.ryan.util.fragments.findListener
import org.thoughtcrime.securesms.ryan.util.visible

/**
 * Fragment that displays who viewed a given story. This is only available if
 * the sender is self.
 */
class StoryViewsFragment :
  DSLSettingsFragment(
    layoutId = R.layout.stories_views_fragment
  ),
  StoryViewsAndRepliesPagerChild {

  private val viewModel: StoryViewsViewModel by viewModels(
    factoryProducer = {
      StoryViewsViewModel.Factory(storyId, StoryViewsRepository())
    }
  )

  private val storyId: Long
    get() = requireArguments().getLong(ARG_STORY_ID)

  private val lifecycleDisposable = LifecycleDisposable()

  override fun bindAdapter(adapter: MappingAdapter) {
    StoryViewItem.register(adapter)

    lifecycleDisposable.bindTo(viewLifecycleOwner)

    val emptyNotice: View = requireView().findViewById(R.id.empty_notice)
    val disabledNotice: View = requireView().findViewById(R.id.disabled_notice)
    val disabledButton: View = requireView().findViewById(R.id.disabled_button)

    disabledButton.setOnClickListener {
      startActivity(StorySettingsActivity.getIntent(requireContext()))
    }

    onPageSelected(findListener<StoryViewsAndRepliesPagerParent>()?.selectedChild ?: StoryViewsAndRepliesPagerParent.Child.VIEWS)

    viewModel.state.observe(viewLifecycleOwner) {
      emptyNotice.visible = it.loadState == StoryViewsState.LoadState.READY && it.views.isEmpty()
      disabledNotice.visible = it.loadState == StoryViewsState.LoadState.DISABLED
      recyclerView?.visible = it.loadState == StoryViewsState.LoadState.READY
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refresh()
  }

  override fun onPageSelected(child: StoryViewsAndRepliesPagerParent.Child) {
    recyclerView?.isNestedScrollingEnabled = child == StoryViewsAndRepliesPagerParent.Child.VIEWS
  }

  private fun getConfiguration(state: StoryViewsState): DSLConfiguration {
    return configure {
      state.views.sortedBy { it.recipient.getDisplayName(requireContext()).lowercase() }.forEach { storyViewItemData ->
        customPref(
          StoryViewItem.Model(
            storyViewItemData = storyViewItemData,
            canRemoveMember = state.storyRecipient?.isDistributionList == true,
            goToChat = { model ->
              lifecycleDisposable += ConversationIntents.createBuilder(requireContext(), model.storyViewItemData.recipient.id, -1L).subscribeBy {
                val chatIntent = it.build()
                startActivity(chatIntent)
              }
            },
            removeFromStory = {
              if (state.storyRecipient?.isDistributionList == true) {
                confirmRemoveFromStory(it.storyViewItemData.recipient, state.storyRecipient)
              }
            }
          )
        )
      }
    }
  }

  private fun confirmRemoveFromStory(user: Recipient, story: Recipient) {
    MaterialAlertDialogBuilder(requireContext())
      .setTitle(R.string.StoryViewsFragment__remove_viewer)
      .setMessage(getString(R.string.StoryViewsFragment__s_will_still_be_able, user.getShortDisplayName(requireContext()), story.getDisplayName(requireContext())))
      .setPositiveButton(R.string.StoryViewsFragment__remove) { _, _ ->
        viewModel.removeUserFromStory(user, story)
      }
      .setNegativeButton(android.R.string.cancel) { _, _ -> }
      .show()
  }

  companion object {
    private const val ARG_STORY_ID = "arg.story.id"

    fun create(storyId: Long): Fragment {
      return StoryViewsFragment().apply {
        arguments = Bundle().apply {
          putLong(ARG_STORY_ID, storyId)
        }
      }
    }
  }
}
