package org.thoughtcrime.securesms.ryan.mediasend.v2.text.send

import android.graphics.Bitmap
import android.net.Uri
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.Base64
import org.signal.core.util.ThreadUtil
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.contacts.paged.ContactSearchKey
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.database.model.StoryType
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.StoryTextPost
import org.thoughtcrime.securesms.ryan.fonts.TextFont
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.keyvalue.StorySend
import org.thoughtcrime.securesms.ryan.linkpreview.LinkPreview
import org.thoughtcrime.securesms.ryan.mediasend.v2.UntrustedRecords
import org.thoughtcrime.securesms.ryan.mediasend.v2.text.TextStoryPostCreationState
import org.thoughtcrime.securesms.ryan.mms.OutgoingMessage
import org.thoughtcrime.securesms.ryan.providers.BlobProvider
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.stories.Stories
import java.io.ByteArrayOutputStream

private val TAG = Log.tag(TextStoryPostSendRepository::class.java)

class TextStoryPostSendRepository {

  fun compressToBlob(bitmap: Bitmap): Single<Uri> {
    return Single.fromCallable {
      val outputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
      bitmap.recycle()
      BlobProvider.getInstance().forData(outputStream.toByteArray()).createForSingleUseInMemory()
    }.subscribeOn(Schedulers.computation())
  }

  fun send(contactSearchKey: Set<ContactSearchKey>, textStoryPostCreationState: TextStoryPostCreationState, linkPreview: LinkPreview?, identityChangesSince: Long): Single<TextStoryPostSendResult> {
    return UntrustedRecords
      .checkForBadIdentityRecords(contactSearchKey.filterIsInstance(ContactSearchKey.RecipientSearchKey::class.java).toSet(), identityChangesSince)
      .toSingleDefault<TextStoryPostSendResult>(TextStoryPostSendResult.Success)
      .onErrorReturn {
        if (it is UntrustedRecords.UntrustedRecordsException) {
          TextStoryPostSendResult.UntrustedRecordsError(it.untrustedRecords)
        } else {
          Log.w(TAG, "Unexpected error occurred", it)
          TextStoryPostSendResult.Failure
        }
      }
      .flatMap { result ->
        if (result is TextStoryPostSendResult.Success) {
          performSend(contactSearchKey, textStoryPostCreationState, linkPreview)
        } else {
          Single.just(result)
        }
      }
  }

  private fun performSend(contactSearchKey: Set<ContactSearchKey>, textStoryPostCreationState: TextStoryPostCreationState, linkPreview: LinkPreview?): Single<TextStoryPostSendResult> {
    return Single.fromCallable {
      val messages: MutableList<OutgoingMessage> = mutableListOf()
      val distributionListSentTimestamp = System.currentTimeMillis()

      for (contact in contactSearchKey) {
        val recipient = Recipient.resolved(contact.requireShareContact().recipientId.get())
        val isStory = contact.requireRecipientSearchKey().isStory || recipient.isDistributionList

        if (isStory && !recipient.isMyStory) {
          SignalStore.story.setLatestStorySend(StorySend.newSend(recipient))
        }

        val storyType: StoryType = when {
          recipient.isDistributionList -> SignalDatabase.distributionLists.getStoryType(recipient.requireDistributionListId())
          isStory -> StoryType.STORY_WITH_REPLIES
          else -> StoryType.NONE
        }

        val message = OutgoingMessage(
          recipient = recipient,
          body = serializeTextStoryState(textStoryPostCreationState),
          timestamp = if (recipient.isDistributionList) distributionListSentTimestamp else System.currentTimeMillis(),
          storyType = storyType.toTextStoryType(),
          previews = listOfNotNull(linkPreview),
          isSecure = true
        )

        messages.add(message)
        ThreadUtil.sleep(5)
      }

      Stories.sendTextStories(messages)
    }.flatMap { messages ->
      messages.toSingleDefault<TextStoryPostSendResult>(TextStoryPostSendResult.Success)
    }
  }

  private fun serializeTextStoryState(textStoryPostCreationState: TextStoryPostCreationState): String {
    val builder = StoryTextPost.Builder()

    builder.body = textStoryPostCreationState.body.toString()
    builder.background = textStoryPostCreationState.backgroundColor.serialize()
    builder.style = when (textStoryPostCreationState.textFont) {
      TextFont.REGULAR -> StoryTextPost.Style.REGULAR
      TextFont.BOLD -> StoryTextPost.Style.BOLD
      TextFont.SERIF -> StoryTextPost.Style.SERIF
      TextFont.SCRIPT -> StoryTextPost.Style.SCRIPT
      TextFont.CONDENSED -> StoryTextPost.Style.CONDENSED
    }
    builder.textBackgroundColor = textStoryPostCreationState.textBackgroundColor
    builder.textForegroundColor = textStoryPostCreationState.textForegroundColor

    return Base64.encodeWithPadding(builder.build().encode())
  }
}
