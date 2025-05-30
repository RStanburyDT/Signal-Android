package org.thoughtcrime.securesms.ryan.stories.settings.create

import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.ViewBinderDelegate
import org.thoughtcrime.securesms.ryan.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.ryan.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.ryan.components.settings.configure
import org.thoughtcrime.securesms.ryan.databinding.StoriesCreateWithRecipientsFragmentBinding
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.util.Material3OnScrollHelper
import org.thoughtcrime.securesms.ryan.util.ViewUtil
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.ryan.util.fragments.findListener
import org.thoughtcrime.securesms.ryan.util.fragments.requireListener
import org.thoughtcrime.securesms.ryan.util.viewholders.RecipientMappingModel
import org.thoughtcrime.securesms.ryan.util.viewholders.RecipientViewHolder

/**
 * Creates a new distribution list with the passed set of viewers and entered distribution label.
 */
class CreateStoryWithViewersFragment : DSLSettingsFragment(
  titleId = R.string.CreateStoryWithViewersFragment__name_story,
  layoutId = R.layout.stories_create_with_recipients_fragment
) {

  companion object {
    const val REQUEST_KEY = "new-story"
    const val STORY_RECIPIENT = "story-recipient"
  }

  private val viewModel: CreateStoryWithViewersViewModel by viewModels(
    factoryProducer = {
      CreateStoryWithViewersViewModel.Factory(CreateStoryWithViewersRepository())
    }
  )

  private val binding by ViewBinderDelegate(StoriesCreateWithRecipientsFragmentBinding::bind)
  private val disposables = LifecycleDisposable()

  private val recipientIds: Array<RecipientId>
    get() = CreateStoryWithViewersFragmentArgs.fromBundle(requireArguments()).recipients

  override fun bindAdapter(adapter: MappingAdapter) {
    adapter.registerFactory(RecipientMappingModel.RecipientIdMappingModel::class.java, LayoutFactory({ RecipientViewHolder(it, null) }, R.layout.stories_recipient_item))

    binding.create.setOnClickListener { viewModel.create(recipientIds.toSet()) }
    binding.create.setCanPress(false)

    val nameViewHolder = CreateStoryNameFieldItem.ViewHolder(binding.nameField.root) {
      viewModel.setLabel(it)
      binding.create.setCanPress(it.isNotBlank())
    }

    disposables.bindTo(viewLifecycleOwner)
    adapter.submitList(getConfiguration().toMappingModelList())
    disposables += viewModel.state.subscribe { state ->

      val nameModel = CreateStoryNameFieldItem.Model(
        body = state.label,
        error = presentError(state.error)
      )

      nameViewHolder.bind(nameModel)

      when (state.saveState) {
        CreateStoryWithViewersState.SaveState.Init -> binding.create.setCanPress(state.label.isNotBlank())
        CreateStoryWithViewersState.SaveState.Saving -> binding.create.setCanPress(false)
        is CreateStoryWithViewersState.SaveState.Saved -> onDone(state.saveState.recipientId)
      }
    }

    Material3OnScrollHelper(
      activity = requireActivity(),
      setStatusBarColor = { requireListener<Callback>().setStatusBarColor(it) },
      getStatusBarColor = { requireListener<Callback>().getStatusBarColor() },
      views = listOf(binding.toolbar),
      lifecycleOwner = viewLifecycleOwner
    ).attach(binding.appBarLayout)
    ViewUtil.focusAndShowKeyboard(binding.nameField.editText)
  }

  override fun onPause() {
    super.onPause()
    ViewUtil.hideKeyboard(requireContext(), binding.nameField.editText)
  }

  override fun onToolbarNavigationClicked() {
    findNavController().popBackStack()
  }

  private fun View.setCanPress(canPress: Boolean) {
    isEnabled = canPress
    alpha = if (canPress) 1f else 0.5f
  }

  override fun getMaterial3OnScrollHelper(toolbar: Toolbar?): Material3OnScrollHelper? {
    return null
  }

  private fun getConfiguration(): DSLConfiguration {
    return configure {
      dividerPref()

      sectionHeaderPref(R.string.CreateStoryWithViewersFragment__viewers)

      recipientIds.forEach {
        customPref(RecipientMappingModel.RecipientIdMappingModel(it))
      }
    }
  }

  private fun presentError(error: CreateStoryWithViewersState.NameError?): String? {
    return when (error) {
      CreateStoryWithViewersState.NameError.NO_LABEL -> getString(R.string.CreateStoryWithViewersFragment__this_field_is_required)
      CreateStoryWithViewersState.NameError.DUPLICATE_LABEL -> getString(R.string.CreateStoryWithViewersFragment__there_is_already_a_story_with_this_name)
      else -> null
    }
  }

  private fun onDone(recipientId: RecipientId) {
    val callback: Callback? = findListener<Callback>()
    if (callback != null) {
      callback.onDone(recipientId)
    } else {
      setFragmentResult(
        REQUEST_KEY,
        Bundle().apply {
          putParcelable(STORY_RECIPIENT, recipientId)
        }
      )
      findNavController().popBackStack(R.id.createStoryViewerSelection, true)
    }
  }

  interface Callback {
    fun setStatusBarColor(@ColorInt color: Int)
    fun getStatusBarColor(): Int
    fun onDone(recipientId: RecipientId)
  }
}
