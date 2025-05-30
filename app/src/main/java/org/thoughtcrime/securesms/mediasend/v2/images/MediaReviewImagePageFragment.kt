package org.thoughtcrime.securesms.ryan.mediasend.v2.images

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.reactivex.rxjava3.disposables.Disposable
import org.signal.core.util.getParcelableCompat
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.mediasend.v2.HudCommand
import org.thoughtcrime.securesms.ryan.mediasend.v2.MediaSelectionViewModel
import org.thoughtcrime.securesms.ryan.scribbles.ImageEditorFragment
import org.thoughtcrime.securesms.ryan.scribbles.ImageEditorHudV2
import java.util.concurrent.TimeUnit

private const val IMAGE_EDITOR_TAG = "image.editor.fragment"

private val MODE_DELAY = TimeUnit.MILLISECONDS.toMillis(300)

/**
 * Displays the chosen image within the image editor. Also manages the "touch enabled" state of the shared
 * view model. We utilize delays here to help with Animation choreography.
 */
class MediaReviewImagePageFragment : Fragment(R.layout.fragment_container), ImageEditorFragment.Controller {

  private val sharedViewModel: MediaSelectionViewModel by viewModels(ownerProducer = { requireActivity() })

  private var imageEditorFragment: ImageEditorFragment? = null
  private var hudCommandDisposable: Disposable = Disposable.disposed()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    imageEditorFragment = ensureImageEditorFragment()
  }

  override fun onPause() {
    super.onPause()

    hudCommandDisposable.dispose()
  }

  override fun onResume() {
    super.onResume()

    hudCommandDisposable = sharedViewModel.hudCommands.subscribe { command ->
      if (isResumed) {
        when (command) {
          HudCommand.StartDraw -> {
            sharedViewModel.setTouchEnabled(false)
            requireView().postDelayed(
              {
                imageEditorFragment?.setMode(ImageEditorHudV2.Mode.DRAW)
              },
              MODE_DELAY
            )
          }
          HudCommand.StartCropAndRotate -> {
            sharedViewModel.setTouchEnabled(false)
            requireView().postDelayed(
              {
                imageEditorFragment?.setMode(ImageEditorHudV2.Mode.CROP)
              },
              MODE_DELAY
            )
          }
          HudCommand.SaveMedia -> imageEditorFragment?.onSave()
          else -> Unit
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    imageEditorFragment?.let {
      sharedViewModel.setEditorState(requireUri(), requireNotNull(it.saveState()))
    }
  }

  private fun ensureImageEditorFragment(): ImageEditorFragment {
    val fragmentInManager: ImageEditorFragment? = childFragmentManager.findFragmentByTag(IMAGE_EDITOR_TAG) as? ImageEditorFragment

    return if (fragmentInManager != null) {
      fragmentInManager
    } else {
      val imageEditorFragment = ImageEditorFragment.newInstance(
        requireUri()
      )

      childFragmentManager.beginTransaction()
        .replace(
          R.id.fragment_container,
          imageEditorFragment,
          IMAGE_EDITOR_TAG
        )
        .commitAllowingStateLoss()

      imageEditorFragment
    }
  }

  private fun requireUri(): Uri = requireNotNull(requireArguments().getParcelableCompat(ARG_URI, Uri::class.java))

  override fun onTouchEventsNeeded(needed: Boolean) {
    if (isResumed) {
      if (!needed) {
        requireView().postDelayed(
          {
            sharedViewModel.setTouchEnabled(true)
          },
          MODE_DELAY
        )
      } else {
        sharedViewModel.setTouchEnabled(false)
      }
    }
  }

  override fun onRequestFullScreen(fullScreen: Boolean, hideKeyboard: Boolean) = Unit

  override fun onDoneEditing() {
    imageEditorFragment?.setMode(ImageEditorHudV2.Mode.NONE)

    if (isResumed) {
      imageEditorFragment?.let {
        sharedViewModel.setEditorState(requireUri(), requireNotNull(it.saveState()))
      }
    }
  }

  override fun onCancelEditing() {
    restoreState()
  }

  override fun onMainImageLoaded() {
    sharedViewModel.sendCommand(HudCommand.ResumeEntryTransition)
  }

  override fun onMainImageFailedToLoad() {
    sharedViewModel.sendCommand(HudCommand.ResumeEntryTransition)
  }

  override fun restoreState() {
    val data = sharedViewModel.getEditorState(requireUri()) as? ImageEditorFragment.Data

    if (data != null) {
      imageEditorFragment?.restoreState(data)
    } else {
      imageEditorFragment?.onClearAll()
    }
  }

  companion object {
    private const val ARG_URI = "arg.uri"

    fun newInstance(uri: Uri): Fragment {
      return MediaReviewImagePageFragment().apply {
        arguments = Bundle().apply {
          putParcelable(ARG_URI, uri)
        }
      }
    }
  }
}
