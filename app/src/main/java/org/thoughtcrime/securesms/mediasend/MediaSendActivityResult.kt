package org.thoughtcrime.securesms.ryan.mediasend

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.signal.core.util.getParcelableExtraCompat
import org.thoughtcrime.securesms.ryan.conversation.MessageSendType
import org.thoughtcrime.securesms.ryan.database.model.Mention
import org.thoughtcrime.securesms.ryan.database.model.StoryType
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.BodyRangeList
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.sms.MessageSender.PreUploadResult
import org.thoughtcrime.securesms.ryan.util.ParcelUtil

/**
 * A class that lets us nicely format data that we'll send back to [ConversationActivity].
 */
@Parcelize
class MediaSendActivityResult(
  val recipientId: RecipientId,
  val preUploadResults: List<PreUploadResult> = emptyList(),
  val nonUploadedMedia: List<Media> = emptyList(),
  val body: String,
  val messageSendType: MessageSendType,
  val isViewOnce: Boolean,
  val mentions: List<Mention>,
  @TypeParceler<BodyRangeList?, BodyRangeListParceler>() val bodyRanges: BodyRangeList?,
  val storyType: StoryType,
  val scheduledTime: Long = -1
) : Parcelable {

  val isPushPreUpload: Boolean
    get() = preUploadResults.isNotEmpty()

  init {
    require((preUploadResults.isNotEmpty() && nonUploadedMedia.isEmpty()) || (preUploadResults.isEmpty() && nonUploadedMedia.isNotEmpty()))
  }

  companion object {
    const val EXTRA_RESULT = "result"

    @JvmStatic
    fun fromData(data: Intent): MediaSendActivityResult {
      return data.getParcelableExtraCompat(EXTRA_RESULT, MediaSendActivityResult::class.java) ?: throw IllegalArgumentException()
    }
  }
}

object BodyRangeListParceler : Parceler<BodyRangeList?> {
  override fun create(parcel: Parcel): BodyRangeList? {
    val data: ByteArray? = ParcelUtil.readByteArray(parcel)
    return if (data != null) {
      BodyRangeList.ADAPTER.decode(data)
    } else {
      null
    }
  }

  override fun BodyRangeList?.write(parcel: Parcel, flags: Int) {
    ParcelUtil.writeByteArray(parcel, this?.encode())
  }
}
