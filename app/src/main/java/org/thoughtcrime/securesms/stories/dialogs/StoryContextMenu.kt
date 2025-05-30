package org.thoughtcrime.securesms.ryan.stories.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.Options
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.core.util.Base64
import org.signal.core.util.DimensionUnit
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.attachments.Attachment
import org.thoughtcrime.securesms.ryan.attachments.AttachmentSaver
import org.thoughtcrime.securesms.ryan.components.menu.ActionItem
import org.thoughtcrime.securesms.ryan.components.menu.SignalContextMenu
import org.thoughtcrime.securesms.ryan.database.model.MessageRecord
import org.thoughtcrime.securesms.ryan.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.StoryTextPost
import org.thoughtcrime.securesms.ryan.providers.BlobProvider
import org.thoughtcrime.securesms.ryan.stories.StoryTextPostModel
import org.thoughtcrime.securesms.ryan.stories.landing.StoriesLandingItem
import org.thoughtcrime.securesms.ryan.stories.viewer.page.StoryPost
import org.thoughtcrime.securesms.ryan.stories.viewer.page.StoryViewerPageState
import org.thoughtcrime.securesms.ryan.util.BitmapUtil
import org.thoughtcrime.securesms.ryan.util.DeleteDialog
import org.thoughtcrime.securesms.ryan.util.MediaUtil
import org.thoughtcrime.securesms.ryan.util.SaveAttachmentUtil
import java.io.ByteArrayInputStream

object StoryContextMenu {

  private val TAG = Log.tag(StoryContextMenu::class.java)

  fun delete(context: Context, records: Set<MessageRecord>): Single<Boolean> {
    return DeleteDialog.show(
      context = context,
      messageRecords = records,
      title = context.getString(R.string.MyStories__delete_story),
      message = context.getString(R.string.MyStories__this_story_will_be_deleted),
      forceRemoteDelete = true
    ).map { (_, deletedThread) -> deletedThread }
  }

  suspend fun save(fragment: Fragment, messageRecord: MessageRecord) {
    val mediaMessageRecord = messageRecord as? MmsMessageRecord
    val uri: Uri? = mediaMessageRecord?.slideDeck?.firstSlide?.uri
    val contentType: String? = mediaMessageRecord?.slideDeck?.firstSlide?.contentType

    when {
      mediaMessageRecord?.storyType?.isTextStory == true -> saveTextStory(fragment, mediaMessageRecord)
      uri == null || contentType == null -> showErrorCantSaveStory(fragment, uri, contentType)
      else -> saveMediaStory(fragment, uri, contentType, mediaMessageRecord)
    }
  }

  private suspend fun saveTextStory(fragment: Fragment, messageRecord: MmsMessageRecord) {
    val saveAttachment = withContext(Dispatchers.Main) {
      val model = StoryTextPostModel.parseFrom(messageRecord)
      val decoder = StoryTextPostModel.Decoder()
      val bitmap = decoder.decode(model, 1080, 1920, Options()).get()
      val jpeg: ByteArrayInputStream = BitmapUtil.toCompressedJpeg(bitmap)

      bitmap.recycle()

      SaveAttachmentUtil.SaveAttachment(
        uri = BlobProvider.getInstance().forData(jpeg.readBytes()).createForSingleUseInMemory(),
        contentType = MediaUtil.IMAGE_JPEG,
        date = messageRecord.dateSent,
        fileName = null
      )
    }

    AttachmentSaver(fragment).saveAttachments(setOf(saveAttachment))
  }

  private suspend fun saveMediaStory(fragment: Fragment, uri: Uri, contentType: String, mediaMessageRecord: MmsMessageRecord) {
    val saveAttachment = SaveAttachmentUtil.SaveAttachment(uri = uri, contentType = contentType, date = mediaMessageRecord.dateSent, fileName = null)
    AttachmentSaver(fragment).saveAttachments(setOf(saveAttachment))
  }

  private fun showErrorCantSaveStory(fragment: Fragment, uri: Uri?, contentType: String?) {
    Log.w(TAG, "Unable to save story media uri: $uri contentType: $contentType")
    Toast.makeText(fragment.requireContext(), R.string.MyStories__unable_to_save, Toast.LENGTH_LONG).show()
  }

  fun share(fragment: Fragment, messageRecord: MmsMessageRecord) {
    val intent = if (messageRecord.storyType.isTextStory) {
      val textStoryBody = StoryTextPost.ADAPTER.decode(Base64.decode(messageRecord.body)).body
      val linkUrl = messageRecord.linkPreviews.firstOrNull()?.url ?: ""
      val shareText = "$textStoryBody $linkUrl".trim()

      ShareCompat.IntentBuilder(fragment.requireContext())
        .setText(shareText)
        .setType("text/plain")
        .createChooserIntent()
    } else {
      val attachment: Attachment = messageRecord.slideDeck.firstSlide!!.asAttachment()

      ShareCompat.IntentBuilder(fragment.requireContext())
        .setStream(attachment.publicUri)
        .setType(attachment.contentType)
        .createChooserIntent()
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
      fragment.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      Log.w(TAG, "No activity existed to share the media.", e)
      Toast.makeText(fragment.requireContext(), R.string.MediaPreviewActivity_cant_find_an_app_able_to_share_this_media, Toast.LENGTH_LONG).show()
    }
  }

  fun show(
    context: Context,
    anchorView: View,
    previewView: View,
    model: StoriesLandingItem.Model,
    onDismiss: () -> Unit
  ) {
    show(
      context = context,
      anchorView = anchorView,
      isFromSelf = model.data.primaryStory.messageRecord.isOutgoing,
      isToGroup = model.data.storyRecipient.isGroup,
      isFromReleaseChannel = model.data.storyRecipient.isReleaseNotes,
      canHide = !model.data.isHidden,
      callbacks = object : Callbacks {
        override fun onHide() = model.onHideStory(model)
        override fun onUnhide() = model.onHideStory(model)
        override fun onForward() = model.onForwardStory(model)
        override fun onShare() = model.onShareStory(model)
        override fun onGoToChat() = model.onGoToChat(model)
        override fun onDismissed() = onDismiss()
        override fun onDelete() = model.onDeleteStory(model)
        override fun onSave() = model.onSave(model)
        override fun onInfo() = model.onInfo(model, previewView)
      }
    )
  }

  fun show(
    context: Context,
    anchorView: View,
    storyViewerPageState: StoryViewerPageState,
    onHide: (StoryPost) -> Unit,
    onUnhide: (StoryPost) -> Unit,
    onForward: (StoryPost) -> Unit,
    onShare: (StoryPost) -> Unit,
    onGoToChat: (StoryPost) -> Unit,
    onSave: (StoryPost) -> Unit,
    onDelete: (StoryPost) -> Unit,
    onInfo: (StoryPost) -> Unit,
    onDismiss: () -> Unit
  ) {
    val selectedStory: StoryPost = storyViewerPageState.posts.getOrNull(storyViewerPageState.selectedPostIndex) ?: return

    show(
      context = context,
      anchorView = anchorView,
      isFromSelf = selectedStory.sender.isSelf,
      isToGroup = selectedStory.group != null,
      isFromReleaseChannel = selectedStory.sender.isReleaseNotes,
      canHide = !selectedStory.sender.shouldHideStory,
      callbacks = object : Callbacks {
        override fun onHide() = onHide(selectedStory)
        override fun onUnhide() = onUnhide(selectedStory)
        override fun onForward() = onForward(selectedStory)
        override fun onShare() = onShare(selectedStory)
        override fun onGoToChat() = onGoToChat(selectedStory)
        override fun onDismissed() = onDismiss()
        override fun onSave() = onSave(selectedStory)
        override fun onDelete() = onDelete(selectedStory)
        override fun onInfo() = onInfo(selectedStory)
      }
    )
  }

  private fun show(
    context: Context,
    anchorView: View,
    isFromSelf: Boolean,
    isToGroup: Boolean,
    isFromReleaseChannel: Boolean,
    rootView: ViewGroup = anchorView.rootView as ViewGroup,
    canHide: Boolean,
    callbacks: Callbacks
  ) {
    val actions = mutableListOf<ActionItem>().apply {
      if (!isFromSelf || isToGroup) {
        if (canHide) {
          add(
            ActionItem(R.drawable.symbol_x_circle_24, context.getString(R.string.StoriesLandingItem__hide_story)) {
              callbacks.onHide()
            }
          )
        } else {
          add(
            ActionItem(R.drawable.symbol_check_circle_24, context.getString(R.string.StoriesLandingItem__unhide_story)) {
              callbacks.onUnhide()
            }
          )
        }
      }

      if (isFromSelf) {
        add(
          ActionItem(R.drawable.symbol_forward_24, context.getString(R.string.StoriesLandingItem__forward)) {
            callbacks.onForward()
          }
        )
        add(
          ActionItem(R.drawable.symbol_share_android_24, context.getString(R.string.StoriesLandingItem__share)) {
            callbacks.onShare()
          }
        )
        add(
          ActionItem(R.drawable.symbol_trash_24, context.getString(R.string.delete)) {
            callbacks.onDelete()
          }
        )
        add(
          ActionItem(R.drawable.symbol_save_android_24, context.getString(R.string.save)) {
            callbacks.onSave()
          }
        )
      }

      if ((isToGroup || !isFromSelf) && !isFromReleaseChannel) {
        add(
          ActionItem(R.drawable.symbol_open_20, context.getString(R.string.StoriesLandingItem__go_to_chat)) {
            callbacks.onGoToChat()
          }
        )
      }

      add(
        ActionItem(R.drawable.symbol_info_24, context.getString(R.string.StoriesLandingItem__info)) {
          callbacks.onInfo()
        }
      )
    }

    SignalContextMenu.Builder(anchorView, rootView)
      .preferredHorizontalPosition(SignalContextMenu.HorizontalPosition.START)
      .onDismiss {
        callbacks.onDismissed()
      }
      .offsetY(DimensionUnit.DP.toPixels(12f).toInt())
      .offsetX(DimensionUnit.DP.toPixels(16f).toInt())
      .show(actions)
  }

  private interface Callbacks {
    fun onHide()
    fun onUnhide()
    fun onForward()
    fun onShare()
    fun onGoToChat()
    fun onDismissed()
    fun onSave()
    fun onDelete()
    fun onInfo()
  }
}
